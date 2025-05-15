use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Instant;
use once_cell::sync::Lazy;
use crate::types::{Theme, HighlightRange, HighlightDelta};
use tree_sitter::Tree;
use crate::text_utils::text_difference_ratio;
use std::hash::{Hash, Hasher};

// Cache para temas
pub static THEME_CACHE: Lazy<Mutex<HashMap<u64, Arc<Theme>>>> = Lazy::new(|| {
    Mutex::new(HashMap::new())
});

// Cache para árboles de sintaxis
pub static PARSE_CACHE: Lazy<Mutex<HashMap<u64, (Arc<Tree>, String)>>> = Lazy::new(|| {
    Mutex::new(HashMap::new())
});

// Estructura para mantener el resultado en cache
#[derive(Debug, Clone)]
pub struct HighlightCache {
    pub tree: Arc<Tree>,
    pub ranges: Vec<HighlightRange>,
    pub delta: HighlightDelta,
    pub input: String,
    pub version: u64,
    pub last_update: std::time::Instant
}

impl HighlightCache {
    pub fn new(tree: Arc<Tree>, ranges: Vec<HighlightRange>, delta: HighlightDelta, input: String) -> Self {
        Self { 
            tree,
            ranges,
            delta,
            input,
            version: 0,
            last_update: Instant::now()
        }
    }

    pub fn matches_input(&self, input: &str) -> bool {
        if self.is_stale() {
            return false;
        }
        text_difference_ratio(&self.input, input) < 0.3
    }
    
    pub fn is_stale(&self) -> bool {
        self.last_update.elapsed().as_secs() > 300 // 5 minutos
    }
}

// Cache para resultados de resaltado por lenguaje
pub static HIGHLIGHT_CACHE: Lazy<Mutex<HashMap<String, Arc<HighlightCache>>>> = 
    Lazy::new(|| Mutex::new(HashMap::new()));

// Cache de parser para evitar recrear el parser
pub static PARSER_CACHE: Lazy<Mutex<HashMap<String, tree_sitter::Parser>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

pub fn get_cached_theme(theme_str: &str) -> Arc<Theme> {
    let mut hasher = std::collections::hash_map::DefaultHasher::new();
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

pub fn get_highlight_cache(language_name: &str) -> Option<Arc<HighlightCache>> {
    HIGHLIGHT_CACHE.lock().unwrap().get(language_name).cloned()
}

pub fn update_highlight_cache(
    language_name: String,
    input_hash: u64,
    new_tree: Arc<Tree>,
    input: String,
    ranges: Vec<HighlightRange>,
    highlight_delta: HighlightDelta,
) {
    let mut parse_cache = PARSE_CACHE.lock().unwrap();
    let mut highlight_cache = HIGHLIGHT_CACHE.lock().unwrap();

    parse_cache.insert(input_hash, (Arc::clone(&new_tree), input.clone()));
    
    let cache = Arc::new(HighlightCache::new(
        Arc::clone(&new_tree),
        ranges.clone(),
        highlight_delta.clone(),
        input.clone()
    ));
    
    highlight_cache.insert(language_name.clone(), Arc::clone(&cache));
}