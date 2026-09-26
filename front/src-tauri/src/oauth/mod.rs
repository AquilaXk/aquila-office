use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct OAuthCallbackPayload {
    pub url: String,
    pub code: Option<String>,
    pub state: Option<String>,
}

#[derive(Default)]
pub struct OAuthState {
    pub active_port: Arc<Mutex<Option<u16>>>,
}

#[tauri::command]
pub fn start_loopback_oauth(app: AppHandle) -> Result<u16, String> {
    let app_clone = app.clone();

    let port = tauri_plugin_oauth::start(move |url| {
        let code = parse_query_param(&url, "code");
        let state = parse_query_param(&url, "state");

        let _ = app_clone.emit(
            "oauth:callback",
            OAuthCallbackPayload {
                url: url.clone(),
                code,
                state,
            },
        );
    })
    .map_err(|e| e.to_string())?;

    Ok(port)
}

pub fn parse_query_param(url: &str, param: &str) -> Option<String> {
    let query_start = url.find('?')?;
    let query = &url[query_start + 1..];
    for pair in query.split('&') {
        let mut parts = pair.split('=');
        if let (Some(k), Some(v)) = (parts.next(), parts.next()) {
            if k == param {
                return Some(urlencoding_decode(v));
            }
        }
    }
    None
}

fn urlencoding_decode(s: &str) -> String {
    s.replace("%20", " ")
        .replace("%2F", "/")
        .replace("%3A", ":")
        .replace("%3D", "=")
        .replace("%26", "&")
        .replace("+", " ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_query_param() {
        let url = "http://localhost:8085/callback?code=4%2F0AY0e-g...&state=xyz123";
        assert_eq!(
            parse_query_param(url, "code"),
            Some("4/0AY0e-g...".to_string())
        );
        assert_eq!(
            parse_query_param(url, "state"),
            Some("xyz123".to_string())
        );
        assert_eq!(parse_query_param(url, "missing"), None);
    }
}
