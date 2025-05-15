use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jlong, jstring};
use tree_sitter::{ffi::TSTree, Parser, Tree};
use tree_sitter_highlight::{HighlightConfiguration, Highlighter};
use std::sync::Arc;
use std::time::Instant;
use std::hash::{Hash, Hasher};
use std::collections::hash_map::DefaultHasher;
use log::{debug, error};

pub mod cache;
pub mod text_utils;
mod logger;
mod code_exec;
mod future;
mod types;
mod caching;
mod theme;
mod highlighting;
mod utils;
mod jni_bridge;

use crate::cache::{HighlightCache, get_cached_theme, PARSE_CACHE, PARSER_CACHE};
use caching::*;
use highlighting::{load_language, process_highlights, try_incremental_highlight};

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_example_lancelot_rust_RustBridge_helloRust(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let hello = "Hello from Rust!";
    let output = env.new_string(hello).unwrap();
    output.into_raw()
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_example_lancelot_rust_RustBridge_freeTree(
    _env: JNIEnv,
    _class: JClass,
    tree_ptr: jlong,
) {
    if tree_ptr != 0 {
        let _tree = unsafe {  let _ = Tree::from_raw(tree_ptr as *mut TSTree); };
    }
}


#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_example_lancelot_rust_RustBridge_highlight(
    mut env: JNIEnv,
    _class: JClass, 
    code: JString,
    language_name: JString,
    highlights_scm: JString,
    injections_scm: JString,
    locals_scm: JString,
    theme_json: JString,
    highlight_names_json: JString
) -> jstring {
    debug!("tokenizeCode: Starting code tokenization");
    
    // Get input strings safely    
    let input = match env.get_string(&code) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting string: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting code: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let language_name = match env.get_string(&language_name) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting language name: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting language name: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // Calculate cache key for inputs
    let mut hasher = DefaultHasher::new();
    input.hash(&mut hasher);
    language_name.hash(&mut hasher);
    let input_hash = hasher.finish();

    // Get highlight names
    let highlight_names: Vec<String> = env.get_string(&highlight_names_json)
        .map(|s| s.to_str().unwrap().to_string())
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_else(|| vec![
            "keyword".to_string(),
            "function".to_string(),
            "type".to_string(),
            "string".to_string(),
            "number".to_string(),
            "comment".to_string(),
            "constant".to_string(),
            "variable".to_string(),
        ]);

    if let Some(cache) = get_highlight_cache(&language_name) {
        if let Some(delta) = try_incremental_highlight(&cache, &input, &highlight_names) {
            return match serde_json::to_string(&delta) {
                Ok(json) => env.new_string(&json).unwrap().into_raw(),
                Err(e) => {
                    error!("Error serializing cached result: {:?}", e);
                    JObject::null().into_raw()
                }
            };
        }
    }

    // Get configuration strings
    let highlights_scm = match env.get_string(&highlights_scm) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting highlights.scm: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting highlights.scm: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let injections_scm = match env.get_string(&injections_scm) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting injections.scm: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting injections.scm: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let locals_scm = match env.get_string(&locals_scm) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting locals.scm: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting locals.scm: {:?}", e);
            return JObject::null().into_raw();
        }
    };
    
    // Load language and prepare parser
    let language = match load_language(&language_name) {
        Ok(l) => l,
        Err(e) => {
            error!("Error loading language: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let theme_str = match env.get_string(&theme_json) {
        Ok(js) => match js.to_str() {
            Ok(s) => s.to_string(),
            Err(e) => {
                error!("Error converting theme JSON: {:?}", e);
                return JObject::null().into_raw();
            }
        },
        Err(e) => {            
            error!("Error getting theme JSON: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // Set up highlighter
    let mut highlighter = Highlighter::new();
    let mut config = match HighlightConfiguration::new(
        language.clone(),
        "highlighter",
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

    // Get or create parser from cache with error handling
    let mut parser = {
        let mut parser_cache = match PARSER_CACHE.lock() {
            Ok(cache) => cache,
            Err(e) => {
                error!("Failed to lock parser cache: {:?}", e);
                return JObject::null().into_raw();
            }
        };
        
        if let Some(cached_parser) = parser_cache.remove(&language_name) {
            debug!("Using cached parser for {}", language_name);
            cached_parser
        } else {
            debug!("Creating new parser for {}", language_name);
            let mut new_parser = Parser::new();
            if let Err(e) = new_parser.set_language(&language) {
                error!("Failed to set language: {:?}", e);
                return JObject::null().into_raw();
            }
            new_parser
        }
    };
    
    let old_tree = PARSE_CACHE.lock().unwrap()
        .get(&input_hash)
        .and_then(|(tree, cached_input)| {
            if cached_input == &input {
                Some(Arc::clone(tree))
            } else {
                None
            }
        });

    // Parse with old tree if available
    let new_tree = Arc::new(parser.parse(
        &input,
        old_tree.as_ref().map(|t| &**t)
    ).unwrap());

    // Highlight with new tree
    let highlights = match highlighter.highlight(
        &config,
        input.as_bytes(),
        None,
        |_| None
    ) {
        Ok(h) => h,
        Err(e) => {
            error!("Error highlighting code: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // Process results and update caches
    let theme = get_cached_theme(&theme_str);
    let (highlight_delta, ranges) = process_highlights(highlights, highlight_names, Arc::clone(&new_tree), &input);

    // Update caches atomically with improved error handling and timing
    let start_time = Instant::now();
    
    let parse_cache_result = PARSE_CACHE.lock();
    let highlight_cache_result = HIGHLIGHT_CACHE.lock();
      
    if let (Ok(mut parse_cache), Ok(mut highlight_cache)) = (parse_cache_result, highlight_cache_result) {
        // Update parse cache
        parse_cache.insert(input_hash, (Arc::clone(&new_tree), input.clone()));
        
        // Create and update highlight cache
        let cache = Arc::new(HighlightCache::new(
            Arc::clone(&new_tree),
            ranges.clone(),
            highlight_delta.clone(),
            input.clone()
        ));
        
        highlight_cache.insert(language_name.clone(), Arc::clone(&cache));
        
        let elapsed = start_time.elapsed();
        debug!("Cache update completed in {:?}", elapsed);
    } else {
        error!("Failed to acquire cache locks");
    }

    // Return result
    match serde_json::to_string(&highlight_delta) {
        Ok(json) => env.new_string(&json).unwrap().into_raw(),
        Err(e) => {
            error!("Error serializing result: {:?}", e);
            JObject::null().into_raw()
        }
    }
}

fn get_highlight_cache(language_name: &str) -> Option<Arc<HighlightCache>> {
    HIGHLIGHT_CACHE
        .lock()
        .ok()
        .and_then(|cache| cache.get(language_name).map(Arc::clone))
}

