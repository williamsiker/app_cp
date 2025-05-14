use jni::JNIEnv;
use jni::objects::{JValue, JClass, JObject, JString};
use jni::sys::{jlong, jobject, jstring};
use tree_sitter::{ffi::TSTree, Parser, Language, Tree};
use tree_sitter_highlight::{Highlighter, HighlightConfiguration, HighlightEvent};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};

use log::{debug, error};
use anyhow::Result;

mod logger;
mod code_exec;
mod future;

static THEME_CACHE: Lazy<Mutex<HashMap<u64, Arc<Theme>>>> = Lazy::new(|| {
    Mutex::new(HashMap::new())
});

#[derive(Debug, Serialize, Deserialize)]
pub struct Theme {
    pub theme: HashMap<String, String>
}

impl Theme {
    fn calculate_hash(&self) -> u64 {
        let mut hasher = DefaultHasher::new();
        let mut keys: Vec<_> = self.theme.keys().collect();
        keys.sort(); // Sort keys for consistent hashing
        for key in keys {
            key.hash(&mut hasher);
            self.theme.get(key).unwrap().hash(&mut hasher);
        }
        hasher.finish()
    }
}

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
        unsafe {
            let tree = Tree::from_raw(tree_ptr as *mut TSTree);
            drop(tree); // Llama automáticamente a `tree.delete()`
        }
    }
}

#[derive(Debug, Serialize)]
pub enum HighlightEventType {
    Start { index: usize },
    End,
    Source { start: usize, end: usize }
}

#[derive(Debug, Serialize)]
pub struct HighlightDelta {
    pub events: Vec<HighlightEventType>,
    pub highlight_names: Vec<String>,
    pub theme_hash: u64
}

fn get_cached_theme(theme_str: &str) -> Arc<Theme> {
    let mut hasher = DefaultHasher::new();
    theme_str.hash(&mut hasher);
    let input_hash = hasher.finish();
    
    if let Some(cached_theme) = THEME_CACHE.lock().unwrap().get(&input_hash) {
        return Arc::clone(cached_theme);
    }
    
    let theme: Theme = serde_json::from_str(theme_str).unwrap_or(Theme { 
        theme: HashMap::new()
    });
    
    let theme_arc = Arc::new(theme);
    THEME_CACHE.lock().unwrap().insert(input_hash, Arc::clone(&theme_arc));
    theme_arc
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_example_lancelot_rust_RustBridge_highlight(
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
    
    let input: String = match env.get_string(&code) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting code: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let language_name: String = match env.get_string(&language_name) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting language name: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let highlights_scm: String = match env.get_string(&highlights_scm) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting highlights.scm: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let injections_scm: String = match env.get_string(&injections_scm) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting injections.scm: {:?}", e);
            return JObject::null().into_raw(); 
        }
    };

    let locals_scm: String = match env.get_string(&locals_scm) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting locals.scm: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let language = match load_language(&language_name) {
        Ok(l) => l,
        Err(e) => {
            error!("tokenizeCode: Error loading language: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let highlight_names_str: String = match env.get_string(&highlight_names_json) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting highlight names JSON: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let highlight_names: Vec<String> = match serde_json::from_str(&highlight_names_str) {
        Ok(names) => names,
        Err(e) => {
            error!("tokenizeCode: Error parsing highlight names JSON: {:?}", e);
            vec![
                "keyword".to_string(),
                "function".to_string(),
                "type".to_string(),
                "string".to_string(),
                "number".to_string(),
                "comment".to_string(),
                "constant".to_string(),
                "variable".to_string(),
            ]
        }
    };

    println!("Highlight names: {:?}", highlight_names);

    let mut highlighter = Highlighter::new();
    let mut config = match HighlightConfiguration::new(
        language.into(),
        "highlighter",
        &highlights_scm, 
        &injections_scm,
        &locals_scm,
    ) {
        Ok(c) => c,
        Err(e) => {
            error!("tokenizeCode: Error creating highlighter config: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let highlight_name_refs: Vec<&str> = highlight_names.iter().map(|s| s.as_str()).collect();
    config.configure(&highlight_name_refs);

    let highlights = match highlighter.highlight(
        &config,
        input.as_bytes(),
        None,
        |_| None
    ) {
        Ok(h) => h,
        Err(e) => {
            error!("tokenizeCode: Error highlighting code: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let theme_str: String = match env.get_string(&theme_json) {
        Ok(s) => s.into(),
        Err(e) => {
            error!("tokenizeCode: Error getting theme JSON: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    let theme = get_cached_theme(&theme_str);
    let mut delta_events = Vec::new();

    for event in highlights {
        match event.unwrap() {
            HighlightEvent::Source { start, end } => {
                delta_events.push(HighlightEventType::Source { start, end });
            },
            HighlightEvent::HighlightStart(s) => {
                delta_events.push(HighlightEventType::Start { index: s.0 });
            },
            HighlightEvent::HighlightEnd => {
                delta_events.push(HighlightEventType::End);
            },
        }
    }

    let highlight_delta = HighlightDelta {
        events: delta_events,
        highlight_names,
        theme_hash: theme.calculate_hash()
    };

    // Serialize to JSON
    let delta_json = match serde_json::to_string(&highlight_delta) {
        Ok(json) => json,
        Err(e) => {
            error!("tokenizeCode: Error serializing highlight delta: {:?}", e);
            return JObject::null().into_raw();
        }
    };

    // Convert to Java String
    let result = env.new_string(&delta_json).unwrap();
    result.into_raw()
}

fn load_language(language_name: &str) -> Result<Language> {
    match language_name {
        "cpp" => Ok(tree_sitter_cpp::LANGUAGE.into()),
        "javascript" => Ok(tree_sitter_javascript::LANGUAGE.into()),
        "python" => Ok(tree_sitter_python::LANGUAGE.into()),
        _ => Err(anyhow::anyhow!("Unsupported language: {}", language_name))
    }
}
