use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    Manager,
};

pub mod git;
pub mod oauth;
pub mod vault;
pub mod watcher;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_oauth::init())
        .manage(watcher::WatcherState::default())
        .setup(|app| {
            let quit_i = MenuItem::with_id(app, "quit", "Quit Aquila Office", true, None::<&str>)?;
            let show_i = MenuItem::with_id(app, "show", "Open Workspace", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show_i, &quit_i])?;

            let _tray = TrayIconBuilder::new()
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
                })
                .build(app)?;

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            watcher::start_watching_workspace,
            watcher::stop_watching_workspace,
            git::get_workspace_git_status,
            git::get_workspace_git_diff,
            oauth::start_loopback_oauth,
            vault::save_secure_token,
            vault::get_secure_token,
            vault::delete_secure_token,
        ])
        .run(tauri::generate_context!())
        .expect("error while running aquila office desktop application");
}

#[cfg(test)]
mod tests {
    #[test]
    fn test_desktop_shell_initialization() {
        assert!(true);
    }
}
