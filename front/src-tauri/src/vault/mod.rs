use keyring::Entry;
use std::collections::HashMap;
use std::sync::Mutex;

const SERVICE_NAME: &str = "com.aquila.office";

static TEST_VAULT: Mutex<Option<HashMap<String, String>>> = Mutex::new(None);

fn is_test_vault_enabled() -> bool {
    cfg!(test) || std::env::var("AQUILA_VAULT_IN_MEMORY").is_ok()
}

#[tauri::command]
pub fn save_secure_token(key: String, token: String) -> Result<(), String> {
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
}
