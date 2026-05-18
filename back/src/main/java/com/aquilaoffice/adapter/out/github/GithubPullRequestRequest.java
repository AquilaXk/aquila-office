package com.aquilaoffice.adapter.out.github;

record GithubPullRequestRequest(
        String owner,
        String repository,
        String baseBranch,
        String headBranch,
        String title,
        String body,
        String token) {}
