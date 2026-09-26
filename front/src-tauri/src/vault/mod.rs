use keyring::Entry;
use std::collections::HashMap;
use std::sync::Mutex;

const SERVICE_NAME: &str = "com.aquila.office";

static MOCK_VAULT: Mutex<Option<HashMap<String, String>>> = Mutex::new(None);

fn get_mock_vault() -> &'static Mutex<Option<HashMap<String, String>>> {
    &MOCK_VAULT
}

#[tauri::command]
pub fn save_secure_token(key: String, token: String) -> Result<(), String> {
    if let Ok(entry) = Entry::new(SERVICE_NAME, &key) {
        let _ = entry.set_password(&token);
    }
    let mut lock = get_mock_vault().lock().map_err(|e| e.to_string())?;
    let map = lock.get_or_insert_with(HashMap::new);
    map.insert(key, token);
    Ok(())
}

#[tauri::command]
pub fn get_secure_token(key: String) -> Result<Option<String>, String> {
    if let Ok(entry) = Entry::new(SERVICE_NAME, &key) {
        if let Ok(pwd) = entry.get_password() {
            return Ok(Some(pwd));
        }
    }

    let lock = get_mock_vault().lock().map_err(|e| e.to_string())?;
    if let Some(map) = lock.as_ref() {
        if let Some(val) = map.get(&key) {
            return Ok(Some(val.clone()));
        }
    }

    Ok(None)
}

#[tauri::command]
pub fn delete_secure_token(key: String) -> Result<(), String> {
    if let Ok(entry) = Entry::new(SERVICE_NAME, &key) {
        let _ = entry.delete_credential();
    }
    let mut lock = get_mock_vault().lock().map_err(|e| e.to_string())?;
    if let Some(map) = lock.as_mut() {
        map.remove(&key);
    }
    Ok(())
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
}
