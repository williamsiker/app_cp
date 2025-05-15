pub fn text_difference_ratio(a: &str, b: &str) -> f64 {
    let max_len = a.len().max(b.len()) as f64;
    if max_len == 0.0 {
        return 0.0;
    }
    
    let mut different = 0;
    let mut i = 0;
    let mut j = 0;
    
    while i < a.len() && j < b.len() {
        if a.as_bytes()[i] != b.as_bytes()[j] {
            different += 1;
        }
        i += 1;
        j += 1;
    }
    
    different as f64 / max_len
}

pub fn get_text_changes(old: &str, new: &str) -> Vec<(usize, usize)> {
    let mut changes = Vec::new();
    let mut current_diff_start = None;
    let mut i = 0;
    let mut j = 0;

    while i < old.len() && j < new.len() {
        if old.as_bytes().get(i) != new.as_bytes().get(j) {
            if current_diff_start.is_none() {
                current_diff_start = Some(j);
            }
        } else if let Some(start) = current_diff_start.take() {
            changes.push((start, j));
        }
        i += 1;
        j += 1;
    }

    if let Some(start) = current_diff_start {
        changes.push((start, new.len()));
    }

    if i < old.len() {
        changes.push((j, j));
    } else if j < new.len() {
        changes.push((j, new.len()));
    }

    merge_nearby_changes(changes)
}

pub fn merge_nearby_changes(mut changes: Vec<(usize, usize)>) -> Vec<(usize, usize)> {
    if changes.is_empty() {
        return changes;
    }

    changes.sort_by_key(|k| k.0);
    let mut merged = vec![changes[0]];
    let max_gap = 30;

    for &(start, end) in &changes[1..] {
        let last = merged.last_mut().unwrap();
        if start <= last.1 + max_gap {
            last.1 = last.1.max(end);
        } else {
            merged.push((start, end));
        }
    }

    merged
}

pub fn get_reused_ranges(changed_ranges: &[(usize, usize)], text_len: usize) -> Vec<(usize, usize)> {
    let mut reused = Vec::new();
    let mut last_end = 0;
    
    for &(start, end) in changed_ranges {
        if start > last_end {
            reused.push((last_end, start));
        }
        last_end = end;
    }
    
    if last_end < text_len {
        reused.push((last_end, text_len));
    }
    
    reused
}
