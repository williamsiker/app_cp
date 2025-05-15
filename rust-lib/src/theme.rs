use std::collections::HashMap;
use std::hash::{Hash, Hasher};
use std::collections::hash_map::DefaultHasher;
use std::sync::Arc;
use serde::{Deserialize, Serialize};
use crate::cache::THEME_CACHE;
#[derive(Debug, Serialize, Deserialize)]
pub struct Theme {
    pub theme: HashMap<String, String>
}

impl Theme {
    pub fn calculate_hash(&self) -> u64 {
        let mut hasher = DefaultHasher::new();
        let mut keys: Vec<_> = self.theme.keys().collect();
        keys.sort();
        for key in keys {
            key.hash(&mut hasher);
            self.theme.get(key).unwrap().hash(&mut hasher);
        }
        hasher.finish()
    }
}

pub fn get_cached_theme(theme_str: &str) -> Arc<Theme> {
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
