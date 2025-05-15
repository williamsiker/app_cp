use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;
use tree_sitter::Parser;
use tree_sitter::Tree;

use crate::theme::Theme;
use crate::types::HighlightCache;

pub static THEME_CACHE: Lazy<Mutex<HashMap<u64, Arc<Theme>>>> = Lazy::new(|| {
    Mutex::new(HashMap::new())
});

pub static PARSE_CACHE: Lazy<Mutex<HashMap<u64, (Arc<Tree>, String)>>> = Lazy::new(|| {
    Mutex::new(HashMap::new())
});

pub static HIGHLIGHT_CACHE: Lazy<Mutex<HashMap<String, Arc<HighlightCache>>>> = 
    Lazy::new(|| Mutex::new(HashMap::new()));

pub static PARSER_CACHE: Lazy<Mutex<HashMap<String, Parser>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));
