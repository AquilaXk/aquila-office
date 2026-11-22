use crate::validate_and_canonicalize_dir;
use git2::{Diff, DiffOptions, Repository, StatusOptions};
use serde::{Deserialize, Serialize};
use std::path::{Component, Path};

pub const MAX_STATUS_ENTRIES: usize = 5000;
pub const MAX_DIFF_BYTES: usize = 1_048_576;

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq)]
pub struct GitFileChange {
    pub path: String,
    pub status: String,
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq)]
pub struct GitStatusResult {
    pub branch: String,
    pub is_clean: bool,
    pub files: Vec<GitFileChange>,
}

fn validate_optional_relative_file_path(file_path: Option<&str>) -> Result<Option<String>, String> {
    let Some(raw) = file_path else {
        return Ok(None);
    };
    let trimmed = raw.trim();
    if trimmed.is_empty() {
        return Ok(None);
    }
    if trimmed.contains('\0') {
        return Err("File path must not contain NUL bytes".to_string());
    }
    let normalized = trimmed.replace('\\', "/");
    let path = Path::new(&normalized);
    if path.is_absolute()
        || path.components().any(|component| {
            matches!(
                component,
                Component::ParentDir | Component::RootDir | Component::Prefix(_)
            )
        })
    {
        return Err("File path must stay relative to workspace".to_string());
    }
    Ok(Some(normalized))
}

fn get_workspace_git_status_with_limit(
    path: String,
    max_entries: usize,
) -> Result<GitStatusResult, String> {
    let canonical_dir = validate_and_canonicalize_dir(&path)?;
    let repo = Repository::discover(&canonical_dir).map_err(|e| e.to_string())?;

    let branch = repo
        .head()
        .ok()
        .and_then(|h| h.shorthand().map(|s| s.to_string()))
        .unwrap_or_else(|| "HEAD".to_string());

    let mut opts = StatusOptions::new();
    opts.include_untracked(true);
    opts.recurse_untracked_dirs(true);
    opts.renames_head_to_index(true);
    opts.renames_index_to_workdir(true);

    let statuses = repo.statuses(Some(&mut opts)).map_err(|e| e.to_string())?;
    let mut files = Vec::new();

    for entry in statuses.iter() {
        if files.len() >= max_entries {
            return Err(format!(
                "Git status entry count exceeds maximum allowed ({})",
                max_entries
            ));
        }

        let s = entry.status();
        let file_path = entry.path().unwrap_or("").to_string();

        let status_str = if s.is_wt_new() {
            "untracked"
        } else if s.is_wt_modified() || s.is_index_modified() {
            "modified"
        } else if s.is_wt_deleted() || s.is_index_deleted() {
            "deleted"
        } else if s.is_index_new() {
            "added"
        } else if s.is_wt_renamed() || s.is_index_renamed() {
            "renamed"
        } else {
            "changed"
        };

        files.push(GitFileChange {
            path: file_path,
            status: status_str.to_string(),
        });
    }

    let is_clean = files.is_empty();

    Ok(GitStatusResult {
        branch,
        is_clean,
        files,
    })
}

#[tauri::command]
pub async fn get_workspace_git_status(path: String) -> Result<GitStatusResult, String> {
    tauri::async_runtime::spawn_blocking(move || {
        get_workspace_git_status_with_limit(path, MAX_STATUS_ENTRIES)
    })
    .await
    .map_err(|e| e.to_string())?
}

fn collect_git_diff_bounded(diff: &Diff<'_>, max_bytes: usize) -> Result<String, String> {
    let mut diff_text = Vec::new();
    let mut exceeded = false;
    let print_result = diff.print(git2::DiffFormat::Patch, |_delta, _hunk, line| {
        let origin = line.origin();
        let origin_len = usize::from(matches!(origin, '+' | '-' | ' '));
        let content = line.content();
        let next_len = diff_text
            .len()
            .saturating_add(origin_len)
            .saturating_add(content.len());
        if next_len > max_bytes {
            exceeded = true;
            return false;
        }
        if origin_len == 1 {
            diff_text.push(origin as u8);
        }
        diff_text.extend_from_slice(content);
        true
    });

    if exceeded {
        return Err(format!(
            "Git diff output exceeds maximum allowed ({} bytes)",
            max_bytes
        ));
    }

    print_result.map_err(|e| e.to_string())?;
    String::from_utf8(diff_text).map_err(|e| e.to_string())
}

fn get_workspace_git_diff_with_limit(
    path: String,
    file_path: Option<String>,
    max_bytes: usize,
) -> Result<String, String> {
    let canonical_dir = validate_and_canonicalize_dir(&path)?;
    let validated_file_path = validate_optional_relative_file_path(file_path.as_deref())?;
    let repo = Repository::discover(&canonical_dir).map_err(|e| e.to_string())?;
    let mut diff_opts = DiffOptions::new();
    diff_opts.include_untracked(true);
    diff_opts.show_untracked_content(true);
    diff_opts.recurse_untracked_dirs(true);

    if let Some(ref fp) = validated_file_path {
        diff_opts.pathspec(fp);
    }

    let head_tree = repo.head().ok().and_then(|h| h.peel_to_tree().ok());

    let diff = repo
        .diff_tree_to_workdir_with_index(head_tree.as_ref(), Some(&mut diff_opts))
        .map_err(|e| e.to_string())?;

    collect_git_diff_bounded(&diff, max_bytes)
}

#[tauri::command]
pub async fn get_workspace_git_diff(
    path: String,
    file_path: Option<String>,
) -> Result<String, String> {
    tauri::async_runtime::spawn_blocking(move || {
        get_workspace_git_diff_with_limit(path, file_path, MAX_DIFF_BYTES)
    })
    .await
    .map_err(|e| e.to_string())?
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::{self, File};
    use std::io::Write;

    #[test]
    fn test_git_status_and_diff_in_temp_repo() {
        let temp_dir = std::env::temp_dir().join(format!("aquila_test_git_{}", std::process::id()));
        let _ = fs::remove_dir_all(&temp_dir);
        fs::create_dir_all(&temp_dir).unwrap();

        let repo = Repository::init(&temp_dir).unwrap();
        let test_file = temp_dir.join("hello.txt");
        let mut f = File::create(&test_file).unwrap();
        writeln!(f, "initial line").unwrap();

        let mut index = repo.index().unwrap();
        index.add_path(Path::new("hello.txt")).unwrap();
        index.write().unwrap();

        let sig = repo
            .signature()
            .unwrap_or_else(|_| git2::Signature::now("Aquila Test", "test@aquila.io").unwrap());
        let tree_id = index.write_tree().unwrap();
        let tree = repo.find_tree(tree_id).unwrap();
        repo.commit(Some("HEAD"), &sig, &sig, "Initial commit", &tree, &[])
            .unwrap();

        let mut f = fs::OpenOptions::new()
            .append(true)
            .open(&test_file)
            .unwrap();
        writeln!(f, "second line").unwrap();

        let status = get_workspace_git_status_with_limit(
            temp_dir.to_string_lossy().to_string(),
            MAX_STATUS_ENTRIES,
        )
        .unwrap();
        assert!(!status.is_clean);
        assert_eq!(status.files.len(), 1);
        assert_eq!(status.files[0].path, "hello.txt");
        assert_eq!(status.files[0].status, "modified");

        let diff = get_workspace_git_diff_with_limit(
            temp_dir.to_string_lossy().to_string(),
            None,
            MAX_DIFF_BYTES,
        )
        .unwrap();
        assert!(diff.contains("+second line"));

        // Test untracked file diff
        let untracked_dir = temp_dir.join("subdir");
        fs::create_dir_all(&untracked_dir).unwrap();
        let untracked_file = untracked_dir.join("untracked.txt");
        let mut f2 = File::create(&untracked_file).unwrap();
        writeln!(f2, "brand new untracked content").unwrap();

        let status2 = get_workspace_git_status_with_limit(
            temp_dir.to_string_lossy().to_string(),
            MAX_STATUS_ENTRIES,
        )
        .unwrap();
        let untracked_entry = status2
            .files
            .iter()
            .find(|f| f.path == "subdir/untracked.txt");
        assert!(
            untracked_entry.is_some(),
            "nested untracked file should be detected"
        );
        assert_eq!(untracked_entry.unwrap().status, "untracked");

        let diff_untracked = get_workspace_git_diff_with_limit(
            temp_dir.to_string_lossy().to_string(),
            Some("subdir/untracked.txt".to_string()),
            MAX_DIFF_BYTES,
        )
        .unwrap();
        assert!(diff_untracked.contains("+brand new untracked content"));

        let diff_untracked_backslash = get_workspace_git_diff_with_limit(
            temp_dir.to_string_lossy().to_string(),
            Some("subdir\\untracked.txt".to_string()),
            MAX_DIFF_BYTES,
        )
        .unwrap();
        assert!(diff_untracked_backslash.contains("+brand new untracked content"));

        let err_traversal = get_workspace_git_diff_with_limit(
            temp_dir.to_string_lossy().to_string(),
            Some("../secret.txt".to_string()),
            MAX_DIFF_BYTES,
        )
        .unwrap_err();
        assert!(err_traversal.contains("relative"));

        let _ = fs::remove_dir_all(&temp_dir);
    }

    #[test]
    fn test_git_status_and_diff_fail_closed_limits() {
        assert_eq!(MAX_STATUS_ENTRIES, 5000);
        assert_eq!(MAX_DIFF_BYTES, 1_048_576);

        let temp_dir =
            std::env::temp_dir().join(format!("aquila_test_git_limits_{}", std::process::id()));
        let _ = fs::remove_dir_all(&temp_dir);
        fs::create_dir_all(&temp_dir).unwrap();

        let repo = Repository::init(&temp_dir).unwrap();
        for idx in 0..3 {
            let mut file = File::create(temp_dir.join(format!("file_{idx}.txt"))).unwrap();
            writeln!(file, "payload line {}", "x".repeat(64)).unwrap();
        }

        let status_err =
            get_workspace_git_status_with_limit(temp_dir.to_string_lossy().to_string(), 2)
                .unwrap_err();
        assert!(status_err.contains("exceeds maximum allowed"));

        let diff_err =
            get_workspace_git_diff_with_limit(temp_dir.to_string_lossy().to_string(), None, 40)
                .unwrap_err();
        assert!(diff_err.contains("exceeds maximum allowed"));
        drop(repo);
        let _ = fs::remove_dir_all(&temp_dir);
    }
}
