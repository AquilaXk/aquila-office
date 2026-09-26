use git2::{DiffOptions, Repository, StatusOptions};
use serde::{Deserialize, Serialize};
use std::path::Path;

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

#[tauri::command]
pub fn get_workspace_git_status(path: String) -> Result<GitStatusResult, String> {
    let repo = Repository::discover(Path::new(&path)).map_err(|e| e.to_string())?;

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
pub fn get_workspace_git_diff(
    path: String,
    file_path: Option<String>,
) -> Result<String, String> {
    let repo = Repository::discover(Path::new(&path)).map_err(|e| e.to_string())?;
    let mut diff_opts = DiffOptions::new();
    diff_opts.include_untracked(true);
    diff_opts.show_untracked_content(true);
    diff_opts.recurse_untracked_dirs(true);

    if let Some(ref fp) = file_path {
        diff_opts.pathspec(fp);
    }

    let head_tree = repo.head().ok().and_then(|h| h.peel_to_tree().ok());

    let diff = repo
        .diff_tree_to_workdir_with_index(head_tree.as_ref(), Some(&mut diff_opts))
        .map_err(|e| e.to_string())?;

    let mut diff_text = Vec::new();
    diff.print(git2::DiffFormat::Patch, |_delta, _hunk, line| {
        let origin = line.origin();
        match origin {
            '+' | '-' | ' ' => {
                diff_text.push(origin as u8);
            }
            _ => {}
        }
        diff_text.extend_from_slice(line.content());
        true
    })
    .map_err(|e| e.to_string())?;

    String::from_utf8(diff_text).map_err(|e| e.to_string())
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

        let sig = repo.signature().unwrap_or_else(|_| git2::Signature::now("Aquila Test", "test@aquila.io").unwrap());
        let tree_id = index.write_tree().unwrap();
        let tree = repo.find_tree(tree_id).unwrap();
        repo.commit(Some("HEAD"), &sig, &sig, "Initial commit", &tree, &[]).unwrap();

        let mut f = fs::OpenOptions::new().append(true).open(&test_file).unwrap();
        writeln!(f, "second line").unwrap();

        let status = get_workspace_git_status(temp_dir.to_string_lossy().to_string()).unwrap();
        assert!(!status.is_clean);
        assert_eq!(status.files.len(), 1);
        assert_eq!(status.files[0].path, "hello.txt");
        assert_eq!(status.files[0].status, "modified");

        let diff = get_workspace_git_diff(temp_dir.to_string_lossy().to_string(), None).unwrap();
        assert!(diff.contains("+second line"));

        // Test untracked file diff
        let untracked_dir = temp_dir.join("subdir");
        fs::create_dir_all(&untracked_dir).unwrap();
        let untracked_file = untracked_dir.join("untracked.txt");
        let mut f2 = File::create(&untracked_file).unwrap();
        writeln!(f2, "brand new untracked content").unwrap();

        let status2 = get_workspace_git_status(temp_dir.to_string_lossy().to_string()).unwrap();
        let untracked_entry = status2.files.iter().find(|f| f.path == "subdir/untracked.txt");
        assert!(untracked_entry.is_some(), "nested untracked file should be detected");
        assert_eq!(untracked_entry.unwrap().status, "untracked");

        let diff_untracked = get_workspace_git_diff(
            temp_dir.to_string_lossy().to_string(),
            Some("subdir/untracked.txt".to_string()),
        ).unwrap();
        assert!(diff_untracked.contains("+brand new untracked content"));

        let _ = fs::remove_dir_all(&temp_dir);
    }
}
