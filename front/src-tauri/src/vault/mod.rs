use keyring::Entry;
#[cfg(test)]
use std::collections::HashMap;
#[cfg(test)]
use std::sync::Mutex;

const SERVICE_NAME: &str = "com.aquila.office";
pub const MAX_VAULT_KEY_BYTES: usize = 256;
pub const MAX_VAULT_TOKEN_BYTES: usize = 16_384;

#[cfg(test)]
static TEST_VAULT: Mutex<Option<HashMap<String, String>>> = Mutex::new(None);

#[cfg(test)]
fn is_test_vault_enabled() -> bool {
    // Allow AQUILA_VAULT_IN_MEMORY opt-in solely under cfg(test); production builds always use OS keychain.
    std::env::var_os("AQUILA_VAULT_IN_MEMORY").is_some() || cfg!(test)
}

fn validate_vault_key(key: &str) -> Result<(), String> {
    if key.is_empty() || key.len() > MAX_VAULT_KEY_BYTES {
        return Err(format!(
            "Vault key length must be 1..={} bytes",
            MAX_VAULT_KEY_BYTES
        ));
    }
    Ok(())
}

fn validate_vault_token(token: &str) -> Result<(), String> {
    if token.len() > MAX_VAULT_TOKEN_BYTES {
        return Err(format!(
            "Vault token length exceeds maximum ({} bytes)",
            MAX_VAULT_TOKEN_BYTES
        ));
    }
    Ok(())
}

#[tauri::command]
pub fn save_secure_token(key: String, token: String) -> Result<(), String> {
    validate_vault_key(&key)?;
    validate_vault_token(&token)?;

    #[cfg(test)]
    if is_test_vault_enabled() {
        let mut lock = TEST_VAULT.lock().map_err(|e| e.to_string())?;
        let map = lock.get_or_insert_with(HashMap::new);
        map.insert(key, token);
        return Ok(());
    }

    let entry = Entry::new(SERVICE_NAME, &key).map_err(|e| e.to_string())?;
    entry
        .set_password(&token)
        .map_err(|e| format!("Failed to store secret in OS keychain: {}", e))
}

#[tauri::command]
pub fn get_secure_token(key: String) -> Result<Option<String>, String> {
    validate_vault_key(&key)?;

    #[cfg(test)]
    if is_test_vault_enabled() {
        let lock = TEST_VAULT.lock().map_err(|e| e.to_string())?;
        if let Some(map) = lock.as_ref() {
            return Ok(map.get(&key).cloned());
        }
        return Ok(None);
    }

    let entry = Entry::new(SERVICE_NAME, &key).map_err(|e| e.to_string())?;
    match entry.get_password() {
        Ok(pwd) => Ok(Some(pwd)),
        Err(keyring::Error::NoEntry) => Ok(None),
        Err(e) => Err(format!("Failed to retrieve secret from OS keychain: {}", e)),
    }
}

#[tauri::command]
pub fn delete_secure_token(key: String) -> Result<(), String> {
    validate_vault_key(&key)?;

    #[cfg(test)]
    if is_test_vault_enabled() {
        let mut lock = TEST_VAULT.lock().map_err(|e| e.to_string())?;
        if let Some(map) = lock.as_mut() {
            map.remove(&key);
        }
        return Ok(());
    }

    let entry = Entry::new(SERVICE_NAME, &key).map_err(|e| e.to_string())?;
    match entry.delete_credential() {
        Ok(_) | Err(keyring::Error::NoEntry) => Ok(()),
        Err(e) => Err(format!("Failed to delete secret from OS keychain: {}", e)),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vault_token_lifecycle() {
        let key = "test_token_key".to_string();
        let value = "sample_secure_secret_value_xyz".to_string();

        save_secure_token(key.clone(), value.clone()).unwrap();
        let retrieved = get_secure_token(key.clone()).unwrap();
        assert_eq!(retrieved, Some(value));

        delete_secure_token(key.clone()).unwrap();
        let after_delete = get_secure_token(key).unwrap();
        assert_eq!(after_delete, None);
    }

    #[test]
    fn test_vault_nonexistent_key_returns_none() {
        let res = get_secure_token("non_existent_key_12345".to_string()).unwrap();
        assert_eq!(res, None);
    }

    #[test]
    fn test_vault_fail_closed_size_limits() {
        assert_eq!(MAX_VAULT_KEY_BYTES, 256);
        assert_eq!(MAX_VAULT_TOKEN_BYTES, 16_384);

        assert!(save_secure_token(String::new(), "abc".to_string()).is_err());
        assert!(get_secure_token(String::new()).is_err());
        assert!(delete_secure_token(String::new()).is_err());

        let max_key = "k".repeat(MAX_VAULT_KEY_BYTES);
        let max_token = "t".repeat(MAX_VAULT_TOKEN_BYTES);
        assert!(save_secure_token(max_key.clone(), max_token).is_ok());
        assert!(delete_secure_token(max_key).is_ok());

        let oversized_key = "k".repeat(MAX_VAULT_KEY_BYTES + 1);
        let oversized_token = "t".repeat(MAX_VAULT_TOKEN_BYTES + 1);
        assert!(save_secure_token(oversized_key.clone(), "ok".to_string()).is_err());
        assert!(get_secure_token(oversized_key.clone()).is_err());
        assert!(delete_secure_token(oversized_key).is_err());
        assert!(save_secure_token("valid_key".to_string(), oversized_token).is_err());
    }
}
