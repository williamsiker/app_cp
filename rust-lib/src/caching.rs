use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;

use crate::cache::HighlightCache;

pub static HIGHLIGHT_CACHE: Lazy<Mutex<HashMap<String, Arc<HighlightCache>>>> = 
    Lazy::new(|| Mutex::new(HashMap::new()));