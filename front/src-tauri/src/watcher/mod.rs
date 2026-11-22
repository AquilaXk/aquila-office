use crate::validate_and_canonicalize_dir;
use notify_debouncer_mini::{new_debouncer, notify::RecursiveMode, DebounceEventResult, Debouncer};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    path::{Component, Path, PathBuf},
    sync::{Arc, Mutex},
    time::Duration,
};
use tauri::{AppHandle, Emitter, State};

pub const MAX_ACTIVE_WATCHERS: usize = 8;
pub const MAX_BATCH_PATHS: usize = 256;

const IGNORED_WATCH_SEGMENTS: [&str; 4] = [".git", "node_modules", "target", ".next"];

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct WorkspaceFileChangeEvent {
    pub paths: Vec<String>,
}

pub struct WatcherState {
    pub watchers: Arc<Mutex<HashMap<String, Debouncer<notify::RecommendedWatcher>>>>,
}

impl Default for WatcherState {
    fn default() -> Self {
        Self {
            watchers: Arc::new(Mutex::new(HashMap::new())),
        }
    }
}

impl WatcherState {
    pub fn clear_all(&self) {
        if let Ok(mut lock) = self.watchers.lock() {
            lock.clear();
        }
    }
}

pub fn should_ignore_watched_path(path: &Path) -> bool {
    path.components().any(|component| match component {
        Component::Normal(part) => part
            .to_str()
            .map(|name| IGNORED_WATCH_SEGMENTS.contains(&name))
            .unwrap_or(false),
        _ => false,
    })
}

pub fn normalize_watch_event_path(path: &Path) -> String {
    path.to_string_lossy().replace('\\', "/")
}

pub fn filter_relevant_paths(paths: &[PathBuf], max_paths: usize) -> Vec<String> {
    paths
        .iter()
        .filter(|path| !should_ignore_watched_path(path))
        .take(max_paths)
        .map(|path| normalize_watch_event_path(path))
        .collect()
}

pub(crate) fn resolve_watch_key(raw_path: &str) -> String {
    match validate_and_canonicalize_dir(raw_path) {
        Ok(canonical) => canonical.to_string_lossy().to_string(),
        Err(_) => crate::simplify_verbatim_path_str(raw_path.trim()),
    }
}

pub fn validate_watcher_capacity(
    active_count: usize,
    already_watching: bool,
    max_watchers: usize,
) -> Result<(), String> {
    if !already_watching && active_count >= max_watchers {
        return Err(format!(
            "Active workspace watcher limit reached (max {})",
            max_watchers
        ));
    }
    Ok(())
}

#[tauri::command]
pub async fn start_watching_workspace(
    app: AppHandle,
    path: String,
    state: State<'_, WatcherState>,
) -> Result<(), String> {
    let watchers = state.watchers.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let canonical_dir = validate_and_canonicalize_dir(&path)?;
        let canonical_key = canonical_dir.to_string_lossy().to_string();

        {
            let lock = watchers.lock().map_err(|e| e.to_string())?;
            validate_watcher_capacity(
                lock.len(),
                lock.contains_key(&canonical_key),
                MAX_ACTIVE_WATCHERS,
            )?;
        }

        let app_handle = app.clone();
        let mut debouncer = new_debouncer(
            Duration::from_millis(500),
            move |res: DebounceEventResult| match res {
                Ok(events) => {
                    let raw_paths: Vec<PathBuf> = events.into_iter().map(|e| e.path).collect();
                    let paths = filter_relevant_paths(&raw_paths, MAX_BATCH_PATHS);
                    if !paths.is_empty() {
                        let _ = app_handle
                            .emit("workspace:file-changed", WorkspaceFileChangeEvent { paths });
                    }
                }
                Err(e) => {
                    eprintln!("watcher error: {:?}", e);
                }
            },
        )
        .map_err(|e| e.to_string())?;

        debouncer
            .watcher()
            .watch(&canonical_dir, RecursiveMode::Recursive)
            .map_err(|e| e.to_string())?;

        let mut lock = watchers.lock().map_err(|e| e.to_string())?;
        validate_watcher_capacity(
            lock.len(),
            lock.contains_key(&canonical_key),
            MAX_ACTIVE_WATCHERS,
        )?;
        lock.insert(canonical_key, debouncer);

        Ok(())
    })
    .await
    .map_err(|e| e.to_string())?
}

#[tauri::command]
pub async fn stop_watching_workspace(
    path: Option<String>,
    state: State<'_, WatcherState>,
) -> Result<(), String> {
    let watchers = state.watchers.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let mut lock = watchers.lock().map_err(|e| e.to_string())?;
        if let Some(p) = path {
            let key = resolve_watch_key(&p);
            lock.remove(&key);
        } else {
            lock.clear();
        }
        Ok(())
    })
    .await
    .map_err(|e| e.to_string())?
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_watcher_state_lifecycle() {
        let state = WatcherState::default();
        let lock = state.watchers.lock().unwrap();
        assert!(lock.is_empty());
    }

    #[test]
    fn test_watcher_ignores_noisy_directories_and_caps_batch() {
        assert_eq!(MAX_ACTIVE_WATCHERS, 8);
        assert_eq!(MAX_BATCH_PATHS, 256);

        let mut raw = vec![
            PathBuf::from("/workspace/.git/index"),
            PathBuf::from("/workspace/node_modules/pkg/index.js"),
            PathBuf::from("/workspace/target/debug/bin"),
            PathBuf::from("/workspace/.next/cache/file"),
        ];
        for idx in 0..300 {
            raw.push(PathBuf::from(format!("/workspace/src/file_{idx}.ts")));
        }

        let filtered = filter_relevant_paths(&raw, MAX_BATCH_PATHS);
        assert_eq!(filtered.len(), MAX_BATCH_PATHS);
        assert!(filtered.iter().all(|p| p.contains("/src/")));
        assert!(should_ignore_watched_path(Path::new("/repo/.git/HEAD")));
        assert!(!should_ignore_watched_path(Path::new("/repo/src/lib.rs")));
        assert_eq!(
            normalize_watch_event_path(Path::new("C:\\repo\\src\\main.rs")),
            "C:/repo/src/main.rs"
        );
        assert_eq!(resolve_watch_key("\\\\?\\C:\\repo\\sub"), "C:\\repo\\sub");
    }

    #[test]
    fn test_watcher_capacity_limit_fails_closed() {
        assert!(validate_watcher_capacity(7, false, MAX_ACTIVE_WATCHERS).is_ok());
        assert!(validate_watcher_capacity(8, true, MAX_ACTIVE_WATCHERS).is_ok());
        let err = validate_watcher_capacity(8, false, MAX_ACTIVE_WATCHERS).unwrap_err();
        assert!(err.contains("limit reached"));
    }
}
