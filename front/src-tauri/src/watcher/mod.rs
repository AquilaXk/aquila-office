use notify_debouncer_mini::{new_debouncer, notify::RecursiveMode, DebounceEventResult, Debouncer};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    path::PathBuf,
    sync::{Arc, Mutex},
    time::Duration,
};
use tauri::{AppHandle, Emitter, State};

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

#[tauri::command]
pub fn start_watching_workspace(
    app: AppHandle,
    path: String,
    state: State<'_, WatcherState>,
) -> Result<(), String> {
    let watch_path = PathBuf::from(&path);
    if !watch_path.exists() {
        return Err(format!("Path does not exist: {}", path));
    }

    let app_handle = app.clone();
    let mut debouncer = new_debouncer(
        Duration::from_millis(500),
        move |res: DebounceEventResult| match res {
            Ok(events) => {
                let paths: Vec<String> = events
                    .into_iter()
                    .map(|e| e.path.to_string_lossy().to_string())
                    .collect();
                let _ = app_handle.emit("workspace:file-changed", WorkspaceFileChangeEvent { paths });
            }
            Err(e) => {
                eprintln!("watcher error: {:?}", e);
            }
        },
    )
    .map_err(|e| e.to_string())?;

    debouncer
        .watcher()
        .watch(&watch_path, RecursiveMode::Recursive)
        .map_err(|e| e.to_string())?;

    let mut lock = state.watchers.lock().map_err(|e| e.to_string())?;
    lock.insert(path, debouncer);

    Ok(())
}

#[tauri::command]
pub fn stop_watching_workspace(
    path: Option<String>,
    state: State<'_, WatcherState>,
) -> Result<(), String> {
    let mut lock = state.watchers.lock().map_err(|e| e.to_string())?;
    if let Some(p) = path {
        lock.remove(&p);
    } else {
        lock.clear();
    }
    Ok(())
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
}
