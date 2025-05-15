use std::sync::Arc;
use std::time::Instant;
use tree_sitter::{Tree, Language};
use tree_sitter_highlight::HighlightEvent;
use log::debug;
use anyhow::Result;

use crate::cache::*;
use crate::types::*;
use crate::utils::*;

// Core highlighting functions

pub fn load_language(language_name: &str) -> Result<Language> {
    match language_name {
        "cpp" => Ok(tree_sitter_cpp::LANGUAGE.into()),
        "javascript" => Ok(tree_sitter_javascript::LANGUAGE.into()),
        "python" => Ok(tree_sitter_python::LANGUAGE.into()),
        _ => Err(anyhow::anyhow!("Unsupported language: {}", language_name))
    }
}

pub fn process_highlights(
    highlights: impl Iterator<Item = Result<HighlightEvent, tree_sitter_highlight::Error>>,
    highlight_names: Vec<String>, 
    tree: Arc<Tree>,
    text: &str
) -> (HighlightDelta, Vec<HighlightRange>) {
    let _start_time = Instant::now(); 
    let estimated_capacity = text.len() / 20; 
    let mut ranges = Vec::with_capacity(estimated_capacity);
    let mut current_type: Option<usize> = None;
    
    debug!("Starting highlight processing for {} bytes", text.len());
    for event_result in highlights {
        match event_result.unwrap() {
            HighlightEvent::Source { start: s, end } => {
                if let Some(typ) = current_type {
                    ranges.push(HighlightRange {
                        start: s,
                        end,
                        highlight_type: typ
                    });
                }
            },
            HighlightEvent::HighlightStart(highlight_info) => {
                current_type = Some(highlight_info.0);
            },
            HighlightEvent::HighlightEnd => {
                current_type = None;
            },
        }
    }

    let cache_key = get_language_from_tree(&tree);
    let mut changed_ranges = vec![(0, text.len())];
    let mut version = 0;
    let mut reused_ranges = None;
    
    if let Ok(cache_guard) = HIGHLIGHT_CACHE.lock() {
        if let Some(cache) = cache_guard.get(&cache_key) {
            if text_difference_ratio(&cache.input, text) < 0.3 {
                changed_ranges = get_text_changes(&cache.input, text);
                reused_ranges = Some(get_reused_ranges(&changed_ranges, text.len()));
                version = cache.version + 1;
            }
        }
    }
    
    let delta = HighlightDelta {
        ranges: ranges.clone(),
        highlight_names,
        reused_ranges,
        version: version + 1,
        changed_ranges
    };

    (delta, ranges)
}

pub fn get_language_from_tree(tree: &Tree) -> String {
    match tree.language().name() {
        Some(name) => name.to_string(),
        None => "unknown".to_string()
    }
}

pub fn try_incremental_highlight(
    cache: &HighlightCache,
    new_text: &str,
    highlight_names: &[String]
) -> Option<HighlightDelta> {
    let start_time = Instant::now();
    
    if text_difference_ratio(&cache.input, new_text) > 0.3 {
        debug!("Texts too different for incremental update");
        return None;
    }
    
    let changed_ranges = get_text_changes(&cache.input, new_text);
    let reused_ranges = get_reused_ranges(&changed_ranges, new_text.len());

    let mut new_ranges = Vec::new();
    let current_highlight_type: Option<usize> = None;

    // Reutilizar rangos no afectados
    let mut old_idx = 0;
    let mut current_pos = 0;

    for &(change_start, change_end) in &changed_ranges {
        // Copiar rangos antes del cambio
        while old_idx < cache.ranges.len() && cache.ranges[old_idx].end <= change_start {
            let old_range = &cache.ranges[old_idx];
            new_ranges.push(old_range.clone());
            current_pos = old_range.end;
            old_idx += 1;
        }

        // Omitir rangos dentro del cambio (se recalcularán)
        while old_idx < cache.ranges.len() && cache.ranges[old_idx].start < change_end {
            old_idx += 1;
        }
        current_pos = change_end;
    }
    // Copiar rangos después del último cambio
    while old_idx < cache.ranges.len() {
        let old_range = &cache.ranges[old_idx];
        new_ranges.push(old_range.clone());
        old_idx += 1;
    }

    // Aquí se necesitaría un mini-resaltador para los `changed_ranges`
    // Por simplicidad, si hay cambios significativos, devolvemos None para un resaltado completo.
    // Una implementación más robusta re-resaltaría solo las zonas afectadas.
    if !changed_ranges.is_empty() {
        // Para este ejemplo, si hay cambios, forzamos un re-highlight completo.
        debug!("Incremental update needed, but falling back to full re-highlight for simplicity.");
        return None; 
    }

    let delta = HighlightDelta {
        ranges: new_ranges,
        highlight_names: highlight_names.to_vec(),
        reused_ranges: Some(reused_ranges),
        version: cache.version + 1,
        changed_ranges
    };

    debug!("Incremental highlight completed in {:?}", start_time.elapsed());
    Some(delta)
}