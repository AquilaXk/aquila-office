use std::path::{Component, Path, PathBuf};
use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    Manager, RunEvent,
};

pub mod git;
pub mod oauth;
pub mod vault;
pub mod watcher;

/// Windows `\\?\` verbatim UNC 프리픽스를 안전한 260자 미만 표준 경로로 단순화한다(dunce 참조 패턴).
pub(crate) fn simplify_verbatim_path_str(raw: &str) -> String {
    if raw.len() >= 260 || raw.ends_with('.') || raw.ends_with(' ') {
        return raw.to_string();
    }
    if let Some(unc_tail) = raw.strip_prefix("\\\\?\\UNC\\") {
        if !unc_tail.is_empty() && !unc_tail.contains('\0') {
            return format!("\\\\{}", unc_tail);
        }
    }
    if let Some(drive_tail) = raw.strip_prefix("\\\\?\\") {
        let bytes = drive_tail.as_bytes();
        if bytes.len() >= 3
            && bytes[0].is_ascii_alphabetic()
            && bytes[1] == b':'
            && bytes[2] == b'\\'
        {
            return drive_tail.to_string();
        }
    }
    raw.to_string()
}

pub(crate) fn simplify_verbatim_path(path: PathBuf) -> PathBuf {
    match path.to_str() {
        Some(raw) => PathBuf::from(simplify_verbatim_path_str(raw)),
        None => path,
    }
}

/// 워크스페이스 디렉토리 경로를 검증(NUL/`..`/leaf symlink 거부)한 뒤 정규화된 경로를 반환한다.
pub(crate) fn validate_and_canonicalize_dir(raw_path: &str) -> Result<PathBuf, String> {
    let trimmed = raw_path.trim();
    if trimmed.is_empty() || trimmed.contains('\0') {
        return Err("Workspace path must not be empty or contain NUL bytes".to_string());
    }
    let candidate = Path::new(trimmed);
    if candidate
        .components()
        .any(|component| matches!(component, Component::ParentDir))
    {
        return Err("Workspace path must not contain '..' traversal segments".to_string());
    }
    let metadata = std::fs::symlink_metadata(candidate)
        .map_err(|e| format!("Workspace path is not accessible: {}", e))?;
    if metadata.file_type().is_symlink() {
        return Err("Workspace path must not be a symbolic link".to_string());
    }
    let canonical = std::fs::canonicalize(candidate)
        .map_err(|e| format!("Failed to canonicalize workspace path: {}", e))?;
    if !canonical.is_dir() {
        return Err("Workspace path must be a directory".to_string());
    }
    Ok(simplify_verbatim_path(canonical))
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_oauth::init())
        .manage(watcher::WatcherState::default())
        .manage(oauth::OAuthState::default())
        .setup(|app| {
            #[cfg(not(target_os = "macos"))]
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_decorations(false);
            }

            let quit_i = MenuItem::with_id(app, "quit", "Quit Aquila Office", true, None::<&str>)?;
            let show_i = MenuItem::with_id(app, "show", "Open Workspace", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show_i, &quit_i])?;

            if let Some(tray) = app.tray_by_id("main-tray") {
                tray.set_menu(Some(menu))?;
                tray.set_show_menu_on_left_click(true)?;
                tray.on_menu_event(|app, event| match event.id.as_ref() {
                    "quit" => {
                        app.exit(0);
                    }
                    "show" => {
                        if let Some(window) = app.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.set_focus();
                        }
                    }
                    _ => {}
                });
            } else {
                let mut builder = TrayIconBuilder::with_id("main-tray")
                    .menu(&menu)
                    .show_menu_on_left_click(true)
                    .on_menu_event(|app, event| match event.id.as_ref() {
                        "quit" => {
                            app.exit(0);
                        }
                        "show" => {
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                        _ => {}
                    });
                if let Some(icon) = app.default_window_icon() {
                    builder = builder.icon(icon.clone());
                }
                let _tray = builder.build(app)?;
            }

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            watcher::start_watching_workspace,
            watcher::stop_watching_workspace,
            git::get_workspace_git_status,
            git::get_workspace_git_diff,
            oauth::start_loopback_oauth,
            oauth::stop_loopback_oauth,
            vault::save_secure_token,
            vault::get_secure_token,
            vault::delete_secure_token,
        ])
        .build(tauri::generate_context!())
        .expect("error while building aquila office desktop application");

    app.run(|app_handle, event| {
        if let RunEvent::Exit = event {
            if let Some(oauth_state) = app_handle.try_state::<oauth::OAuthState>() {
                let _ = oauth_state.cancel_active();
            }
            if let Some(watcher_state) = app_handle.try_state::<watcher::WatcherState>() {
                watcher_state.clear_all();
            }
        }
    });
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;

    #[test]
    fn test_validate_and_canonicalize_dir_rejects_traversal_nul_and_leaf_symlink() {
        assert!(validate_and_canonicalize_dir("").is_err());
        assert!(validate_and_canonicalize_dir("/tmp\0evil").is_err());
        assert!(validate_and_canonicalize_dir("/tmp/../etc").is_err());

        let base = std::env::temp_dir().join(format!("aquila_dir_guard_{}", std::process::id()));
        let _ = fs::remove_dir_all(&base);
        let real_dir = base.join("real");
        fs::create_dir_all(&real_dir).unwrap();

        let validated = validate_and_canonicalize_dir(&real_dir.to_string_lossy()).unwrap();
        assert!(validated.is_dir());

        #[cfg(unix)]
        {
            let link_dir = base.join("link");
            std::os::unix::fs::symlink(&real_dir, &link_dir).unwrap();
            let err = validate_and_canonicalize_dir(&link_dir.to_string_lossy()).unwrap_err();
            assert!(err.contains("symbolic link"));
        }

        #[cfg(windows)]
        {
            let link_dir = base.join("link");
            match std::os::windows::fs::symlink_dir(&real_dir, &link_dir) {
                Ok(()) => {
                    let err =
                        validate_and_canonicalize_dir(&link_dir.to_string_lossy()).unwrap_err();
                    assert!(err.contains("symbolic link"));
                }
                Err(e) if e.raw_os_error() == Some(1314) => {
                    // Windows Developer Mode or SeCreateSymbolicLinkPrivilege absent
                }
                Err(e) => panic!("unexpected symlink_dir failure: {e}"),
            }
        }

        let _ = fs::remove_dir_all(&base);
    }

    #[test]
    fn test_simplify_verbatim_path_strips_windows_unc_prefix_safely() {
        assert_eq!(
            simplify_verbatim_path_str("\\\\?\\C:\\workspaces\\aquila\\repo"),
            "C:\\workspaces\\aquila\\repo"
        );
        assert_eq!(
            simplify_verbatim_path_str("\\\\?\\UNC\\srv\\share\\repo"),
            "\\\\srv\\share\\repo"
        );
        let too_long = format!("\\\\?\\C:\\{}", "a".repeat(260));
        assert_eq!(simplify_verbatim_path_str(&too_long), too_long);
        assert_eq!(
            simplify_verbatim_path_str("\\\\?\\C:\\workspaces\\dev "),
            "\\\\?\\C:\\workspaces\\dev "
        );
        assert_eq!(
            simplify_verbatim_path_str("/workspaces/aquila/repo"),
            "/workspaces/aquila/repo"
        );
    }
}
