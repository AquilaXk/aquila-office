package com.aquilaoffice.application.port.out;

/**
 * GitHub 저장소 작업 후 생성된 branch와 PR 위치를 application layer에 반환한다.
 */
public record GithubRepositoryWorkResult(
        String repositoryOwner,
        String repositoryName,
        String branchName,
        String pullRequestUrl) {

    public GithubRepositoryWorkResult {
        repositoryOwner = requireNotBlank(repositoryOwner, "repositoryOwner");
        repositoryName = requireNotBlank(repositoryName, "repositoryName");
        branchName = requireNotBlank(branchName, "branchName");
        pullRequestUrl = requireNotBlank(pullRequestUrl, "pullRequestUrl");
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
