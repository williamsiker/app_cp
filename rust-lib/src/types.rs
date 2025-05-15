use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tree_sitter::Tree;
use std::hash::{Hash, Hasher};
use std::time::Instant;
use std::collections::hash_map::DefaultHasher;

// Las definiciones de Theme, HighlightRange y HighlightDelta se mantienen solo aquí
#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Theme {
    pub theme: HashMap<String, String>
}

impl Theme {
    pub fn calculate_hash(&self) -> u64 {
        let mut hasher = std::collections::hash_map::DefaultHasher::new();
        let mut keys: Vec<_> = self.theme.keys().collect();
        keys.sort(); // Sort keys for consistent hashing
        for key in keys {
            key.hash(&mut hasher);
            self.theme.get(key).unwrap().hash(&mut hasher);
        }
        hasher.finish()
    }
}

#[derive(Debug, Clone, Serialize)]
pub struct HighlightRange {
    pub start: usize,
    pub end: usize,
    pub highlight_type: usize
}

#[derive(Debug, Clone, Serialize)]
pub struct HighlightDelta {
    pub ranges: Vec<HighlightRange>,
    pub highlight_names: Vec<String>,
    pub reused_ranges: Option<Vec<(usize, usize)>>, // Rangos que se pueden reutilizar
    pub version: u64,
    pub changed_ranges: Vec<(usize, usize)> // Rangos que cambiaron desde la última versión
}

// Eliminar duplicidad de HighlightCacheStore, ya que HighlightCache está en cache.rs
// El resto de structs y enums se mantienen si no están duplicados en otros módulos

// Estado para mantener información sobre el resaltado incremental
#[derive(Debug)]
pub struct IncrementalState {
    pub version: u64,
    pub tree: Option<Arc<Tree>>,
    pub ranges: Vec<HighlightRange>,
    pub input_length: usize
}

impl IncrementalState {
    pub fn new() -> Self {
        Self {
            version: 0,
            tree: None,
            ranges: Vec::new(),
            input_length: 0
        }
    }

    pub fn update(&mut self, new_tree: Arc<Tree>, new_ranges: Vec<HighlightRange>, input_length: usize) {
        self.version += 1;
        self.tree = Some(new_tree);
        self.ranges = new_ranges;
        self.input_length = input_length;
    }

    pub fn calculate_changes(&self, new_ranges: &[HighlightRange]) -> Vec<(usize, usize)> {
        let mut changes = Vec::new();
        let mut old_idx = 0;
        let mut new_idx = 0;

        while old_idx < self.ranges.len() && new_idx < new_ranges.len() {
            let old_range = &self.ranges[old_idx];
            let new_range = &new_ranges[new_idx];

            if old_range.start != new_range.start || 
               old_range.end != new_range.end ||
               old_range.highlight_type != new_range.highlight_type {
                changes.push((
                    old_range.start.min(new_range.start),
                    old_range.end.max(new_range.end)
                ));
            }

            if old_range.end <= new_range.end {
                old_idx += 1;
            }
            if new_range.end <= old_range.end {
                new_idx += 1;
            }
        }

        // Manejar rangos restantes
        while old_idx < self.ranges.len() {
            let old_range = &self.ranges[old_idx];
            changes.push((old_range.start, old_range.end));
            old_idx += 1;
        }

        while new_idx < new_ranges.len() {
            let new_range = &new_ranges[new_idx];
            changes.push((new_range.start, new_range.end));
            new_idx += 1;
        }

        // Combinar rangos solapados
        if !changes.is_empty() {
            changes.sort_by_key(|k| k.0);
            let mut merged = vec![changes[0]];
            for &(start, end) in &changes[1..] {
                let last = merged.last_mut().unwrap();
                if start <= last.1 {
                    last.1 = last.1.max(end);
                } else {
                    merged.push((start, end));
                }
            }
            changes = merged;
        }

        changes
    }
}

#[derive(Debug, Clone)]
pub struct HighlightCache {
    pub tree: Arc<Tree>,
    pub ranges: Vec<HighlightRange>,
    pub delta: HighlightDelta,
    pub input: String,
    pub version: u64,
    pub last_update: Instant
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
        crate::utils::text_difference_ratio(&self.input, input) < 0.3
    }
    
    pub fn is_stale(&self) -> bool {
        self.last_update.elapsed().as_secs() > 300
    }
}

#[derive(Debug, Serialize, Clone)]
pub enum HighlightEventType {
    Start { index: usize },
    End,
    Source { start: usize, end: usize }
}