use std::sync::Arc;
use tree_sitter::Tree;
use crate::types::{HighlightRange};

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