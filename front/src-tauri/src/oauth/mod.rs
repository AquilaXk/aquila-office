use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Emitter, State};

pub const MAX_CALLBACK_URL_LEN: usize = 4000;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct OAuthCallbackPayload {
    pub url: String,
    pub code: Option<String>,
    pub state: Option<String>,
    pub error: Option<String>,
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

impl OAuthState {
    pub fn cancel_active(&self) -> Result<(), String> {
        let mut lock = self.active_port.lock().map_err(|e| e.to_string())?;
        if let Some(port) = lock.take() {
            let _ = tauri_plugin_oauth::cancel(port);
        }
        Ok(())
    }
}

#[tauri::command]
pub async fn start_loopback_oauth(
    app: AppHandle,
    state: State<'_, OAuthState>,
) -> Result<u16, String> {
    let active_port = state.active_port.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let mut lock = active_port.lock().map_err(|e| e.to_string())?;
        if let Some(prev_port) = lock.take() {
            let _ = tauri_plugin_oauth::cancel(prev_port);
        }

        let app_clone = app.clone();
        let active_port_callback = active_port.clone();
        let port = tauri_plugin_oauth::start(move |url| {
            if let Ok(mut guard) = active_port_callback.lock() {
                guard.take();
            }
            if url.len() > MAX_CALLBACK_URL_LEN {
                eprintln!(
                    "oauth callback URL rejected: exceeds {} bytes",
                    MAX_CALLBACK_URL_LEN
                );
                let _ = app_clone.emit(
                    "oauth:callback",
                    OAuthCallbackPayload {
                        url: String::new(),
                        code: None,
                        state: None,
                        error: Some("callback_url_too_long".to_string()),
                    },
                );
                return;
            }
            let code = parse_query_param(&url, "code");
            let oauth_state = parse_query_param(&url, "state");
            let oauth_error = parse_query_param(&url, "error");

            let _ = app_clone.emit(
                "oauth:callback",
                OAuthCallbackPayload {
                    url,
                    code,
                    state: oauth_state,
                    error: oauth_error,
                },
            );
        })
        .map_err(|e| e.to_string())?;

        *lock = Some(port);
        Ok(port)
    })
    .await
    .map_err(|e| e.to_string())?
}

#[tauri::command]
pub async fn stop_loopback_oauth(state: State<'_, OAuthState>) -> Result<(), String> {
    let active_port = state.active_port.clone();
    tauri::async_runtime::spawn_blocking(move || {
        let mut lock = active_port.lock().map_err(|e| e.to_string())?;
        if let Some(port) = lock.take() {
            let _ = tauri_plugin_oauth::cancel(port);
        }
        Ok(())
    })
    .await
    .map_err(|e| e.to_string())?
}

pub fn parse_query_param(url: &str, param: &str) -> Option<String> {
    if url.len() > MAX_CALLBACK_URL_LEN {
        return None;
    }
    let without_fragment = url.split('#').next().unwrap_or(url);
    let query_start = without_fragment.find('?')?;
    let query = &without_fragment[query_start + 1..];
    for pair in query.split('&') {
        if let Some((k, v)) = pair.split_once('=') {
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
        assert_eq!(parse_query_param(url, "state"), Some("xyz123".to_string()));
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
        {
            let lock = state.active_port.lock().unwrap();
            assert!(lock.is_none());
        }
        assert!(state.cancel_active().is_ok());
    }

    #[test]
    fn test_parse_query_param_strips_fragment_and_enforces_url_limit() {
        assert_eq!(MAX_CALLBACK_URL_LEN, 4000);

        let url_with_fragment =
            "http://127.0.0.1:8085/callback?code=auth_code_1&state=csrf_1&error=access_denied#access_token=bad&code=spoofed";
        assert_eq!(
            parse_query_param(url_with_fragment, "code"),
            Some("auth_code_1".to_string())
        );
        assert_eq!(
            parse_query_param(url_with_fragment, "state"),
            Some("csrf_1".to_string())
        );
        assert_eq!(
            parse_query_param(url_with_fragment, "error"),
            Some("access_denied".to_string())
        );
        assert_eq!(parse_query_param(url_with_fragment, "access_token"), None);

        let fragment_only_query = "http://127.0.0.1:8085/callback#?code=spoofed";
        assert_eq!(parse_query_param(fragment_only_query, "code"), None);

        let prefix = "http://127.0.0.1:8085/callback?code=";
        let max_len_url = format!(
            "{}{}",
            prefix,
            "a".repeat(MAX_CALLBACK_URL_LEN - prefix.len())
        );
        assert_eq!(max_len_url.len(), MAX_CALLBACK_URL_LEN);
        assert!(parse_query_param(&max_len_url, "code").is_some());

        let oversized_url = format!(
            "{}{}",
            prefix,
            "a".repeat(MAX_CALLBACK_URL_LEN - prefix.len() + 1)
        );
        assert_eq!(parse_query_param(&oversized_url, "code"), None);
    }
}
