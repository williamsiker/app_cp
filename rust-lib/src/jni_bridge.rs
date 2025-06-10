use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jlong, jstring};
use tree_sitter::{ffi::TSTree, Parser, Tree};
use tree_sitter_highlight::{HighlightConfiguration, Highlighter};
use std::sync::Arc;
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use log::{debug, error};
use std::time::Instant;
use tokio::runtime::Builder;

use crate::code_exec::{execute_code, execute_code_detailed};
use crate::cache::{get_cached_theme, PARSE_CACHE, PARSER_CACHE, get_highlight_cache, update_highlight_cache};
use crate::highlighting::{load_language, process_highlights, try_incremental_highlight};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_helloRust(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let hello = "Hello from Rust!";
    let output = env.new_string(hello).unwrap();
    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_freeTree(
    _env: JNIEnv,
    _class: JClass,
    tree_ptr: jlong,
) {
    if tree_ptr != 0 {
        // Reconstitute the tree from the raw pointer and let it be dropped.
        // This will call its `delete` method if `Tree` implements `Drop` appropriately.
        // tree_sitter::Tree does implement Drop and calls ffi::ts_tree_delete.
        let _ = unsafe { Tree::from_raw(tree_ptr as *mut TSTree) };
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_example_lancelot_rust_RustBridge_highlight(
    mut env: JNIEnv,
    _class: JClass, 
    code: JString,
    language_name_jstr: JString,
    highlights_scm_jstr: JString,
    injections_scm_jstr: JString,
    locals_scm_jstr: JString,
    theme_json_jstr: JString,
    highlight_names_json_jstr: JString
) -> jstring {
    debug!("tokenizeCode: Starting code tokenization");
    
    let input = match env.get_string(&code) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting code string: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting code string from JNI: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let language_name = match env.get_string(&language_name_jstr) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting language_name string: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting language_name string from JNI: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let mut hasher = DefaultHasher::new();
    input.hash(&mut hasher);
    language_name.hash(&mut hasher);
    let input_hash = hasher.finish();

    let highlight_names: Vec<String> = env.get_string(&highlight_names_json_jstr)
        .map_err(|e| error!("Failed to get highlight_names_json: {:?}", e))
        .ok()
        .and_then(|s| s.to_str().map(|s_str| s_str.to_owned()).map_err(|e| error!("Failed to convert highlight_names_json to str: {:?}",e)).ok())
        .and_then(|s_owned| serde_json::from_str::<Vec<String>>(&s_owned).map_err(|e| error!("Failed to parse highlight_names_json: {:?}",e)).ok())
        .unwrap_or_else(|| vec![
            "keyword".to_string(), "function".to_string(), "type".to_string(),
            "string".to_string(), "number".to_string(), "comment".to_string(),
            "constant".to_string(), "variable".to_string(),
        ]);

    if let Some(cache) = get_highlight_cache(&language_name) {
        if cache.matches_input(&input) {
            if let Some(delta) = try_incremental_highlight(&*cache, &input, &highlight_names) {
                return match serde_json::to_string(&delta) {
                    Ok(json) => env.new_string(&json).unwrap().into_raw(),
                    Err(e) => {
                        error!("Error serializing cached result: {:?}", e);
                        JObject::null().into_raw()
                    }
                };
            }
        }
    }

    let highlights_scm = match env.get_string(&highlights_scm_jstr).map(|s| s.to_str().unwrap().to_string()) {
        Ok(s) => s, Err(_) => { error!("Failed to get highlights_scm"); return JObject::null().into_raw(); }
    };
    let injections_scm = match env.get_string(&injections_scm_jstr).map(|s| s.to_str().unwrap().to_string()) {
        Ok(s) => s, Err(_) => { error!("Failed to get injections_scm"); return JObject::null().into_raw(); }
    };
    let locals_scm = match env.get_string(&locals_scm_jstr).map(|s| s.to_str().unwrap().to_string()) {
        Ok(s) => s, Err(_) => { error!("Failed to get locals_scm"); return JObject::null().into_raw(); }
    };
    let theme_json = match env.get_string(&theme_json_jstr).map(|s| s.to_str().unwrap().to_string()) {
        Ok(s) => s, Err(_) => { error!("Failed to get theme_json"); return JObject::null().into_raw(); }
    };

    let language = match load_language(&language_name) {
        Ok(l) => l,
        Err(e) => {
            error!("Error loading language '{}': {:?}", language_name, e);
            return JObject::null().into_raw();
        }
    };

    let mut highlighter = Highlighter::new();
    let mut config = match HighlightConfiguration::new(
        language.clone(),
        "highlighter", // Name for the configuration, can be arbitrary
        &highlights_scm,
        &injections_scm,
        &locals_scm,
    ) {
        Ok(c) => c,
        Err(e) => {
            error!("Error creating highlighter config: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let highlight_name_refs: Vec<&str> = highlight_names.iter().map(|s| s.as_str()).collect();
    config.configure(&highlight_name_refs);

    let old_tree_arc_opt = PARSE_CACHE.lock().unwrap()
        .get(&input_hash)
        .filter(|(_, cached_input)| cached_input == &input)
        .map(|(tree, _)| Arc::clone(tree));

    let new_tree = {
        let mut parser_cache_guard = PARSER_CACHE.lock().unwrap();
        let parser = parser_cache_guard.entry(language_name.clone()).or_insert_with(|| {
            debug!("Creating new parser for {}", language_name);
            let mut p = Parser::new();
            p.set_language(&language).expect("Failed to set language on new parser");
            p
        });
        
        match parser.parse(&input, old_tree_arc_opt.as_ref().map(|t| &**t)) {
            Some(tree) => Arc::new(tree),
            None => {
                error!("Parsing failed for language {} with input snippet: {:.50}", language_name, input);
                return JObject::null().into_raw();
            }
        }
    };

    let highlights_iter = match highlighter.highlight(
        &config,
        input.as_bytes(),
        None, // No injection callback needed for this basic setup
        |_| None // No injection callback
    ) {
        Ok(h) => h,
        Err(e) => {
            error!("Error highlighting code: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let _theme_arc = get_cached_theme(&theme_json); // Theme is cached, but not directly used in process_highlights in this version
    let (highlight_delta, ranges) = process_highlights(highlights_iter, highlight_names.clone(), Arc::clone(&new_tree), &input);

    let start_time = Instant::now();
    update_highlight_cache(language_name.clone(), input_hash, Arc::clone(&new_tree), input.clone(), ranges, highlight_delta.clone());
    debug!("Cache update completed in {:?}", start_time.elapsed());

    match serde_json::to_string(&highlight_delta) {
        Ok(json) => env.new_string(&json).unwrap().into_raw(),
        Err(e) => {
            error!("Error serializing result: {:?}", e);
            JObject::null().into_raw()
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_example_lancelot_rust_RustBridge_realTimeHighlight(
    env: JNIEnv,
    class: JClass,
    code: JString,
    language_name_jstr: JString,
    highlights_scm_jstr: JString,
    injections_scm_jstr: JString,
    locals_scm_jstr: JString,
    theme_json_jstr: JString,
    highlight_names_json_jstr: JString,
) -> jstring {
    // Simple wrapper that reuses the optimized highlight implementation
    Java_com_example_lancelot_rust_RustBridge_highlight(
        env,
        class,
        code,
        language_name_jstr,
        highlights_scm_jstr,
        injections_scm_jstr,
        locals_scm_jstr,
        theme_json_jstr,
        highlight_names_json_jstr,
    )
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_executeCode(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
    language_name: JString,
    input: JString,
) -> jstring {
    let code: String = env.get_string(&code).unwrap().into();
    let language_name: String = env.get_string(&language_name).unwrap().into();
    let input: String = env.get_string(&input).unwrap().into();

    // Creamos un runtime local para ejecutar async
    let rt = Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    let result = rt.block_on(async {
        execute_code(&code, &language_name, &input).await
    });

    // Convertimos resultado a jstring para devolver a Java
    let output = result.unwrap_or_else(|err| err);

    env.new_string(output).unwrap().into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_executeCodeDetailed(
    mut env: JNIEnv,
    _class: JClass,
    code: JString,
    language_name: JString,
    input: JString,
) -> jstring {
    let code: String = env.get_string(&code).unwrap().into();
    let language_name: String = env.get_string(&language_name).unwrap().into();
    let input: String = env.get_string(&input).unwrap().into();

    let rt = Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    let result = rt.block_on(async {
        execute_code_detailed(&code, &language_name, &input).await
    });

    let output = match result {
        Ok(res) => serde_json::to_string(&res).unwrap_or_else(|_| "{}".into()),
        Err(err) => format!("{{\"error\":\"{}\"}}", err.replace('"', "'")),
    };

    env.new_string(output).unwrap().into_raw()
}
