use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter, State};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct OAuthCallbackPayload {
    pub url: String,
    pub code: Option<String>,
    pub state: Option<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct GoogleTokenExchangePayload {
    pub code: String,
    pub client_id: String,
    pub client_secret: Option<String>,
    pub redirect_uri: String,
    pub code_verifier: Option<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct GoogleTokenResponse {
    pub access_token: String,
    pub id_token: Option<String>,
    pub expires_in: Option<u64>,
    pub token_type: Option<String>,
    pub refresh_token: Option<String>,
    pub scope: Option<String>,
}

#[derive(Default)]
pub struct OAuthState {
    pub active_port: Arc<Mutex<Option<u16>>>,
}

#[tauri::command]
pub fn start_loopback_oauth(
    app: AppHandle,
    state: State<'_, OAuthState>,
) -> Result<u16, String> {
    let mut lock = state.active_port.lock().map_err(|e| e.to_string())?;
    if let Some(prev_port) = *lock {
        let _ = tauri_plugin_oauth::cancel(prev_port);
        *lock = None;
    }

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

    *lock = Some(port);
    Ok(port)
}

#[tauri::command]
pub fn stop_loopback_oauth(state: State<'_, OAuthState>) -> Result<(), String> {
    let mut lock = state.active_port.lock().map_err(|e| e.to_string())?;
    if let Some(port) = lock.take() {
        let _ = tauri_plugin_oauth::cancel(port);
    }
    Ok(())
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

pub fn urlencoding_decode(s: &str) -> String {
    let mut result = Vec::new();
    let bytes = s.as_bytes();
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == b'+' {
            result.push(b' ');
            i += 1;
        } else if bytes[i] == b'%' && i + 2 < bytes.len() {
            if let Ok(hex_str) = std::str::from_utf8(&bytes[i + 1..=i + 2]) {
                if let Ok(byte) = u8::from_str_radix(hex_str, 16) {
                    result.push(byte);
                    i += 3;
                    continue;
                }
            }
            result.push(bytes[i]);
            i += 1;
        } else {
            result.push(bytes[i]);
            i += 1;
        }
    }
    String::from_utf8_lossy(&result).to_string()
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

    #[test]
    fn test_urlencoding_decode_comprehensive() {
        assert_eq!(urlencoding_decode("hello+world"), "hello world");
        assert_eq!(urlencoding_decode("4%2f0AY0"), "4/0AY0");
        assert_eq!(urlencoding_decode("4%2F0AY0"), "4/0AY0");
        assert_eq!(urlencoding_decode("a%3Db%26c%3D1"), "a=b&c=1");
        assert_eq!(urlencoding_decode("special%21%40%23"), "special!@#");
        assert_eq!(urlencoding_decode("trailing%"), "trailing%");
    }

    #[test]
    fn test_oauth_state_initialization() {
        let state = OAuthState::default();
        let lock = state.active_port.lock().unwrap();
        assert!(lock.is_none());
    }
}
