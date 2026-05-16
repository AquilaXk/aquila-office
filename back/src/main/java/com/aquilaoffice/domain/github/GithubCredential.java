package com.aquilaoffice.domain.github;

/**
 * 워크스페이스에서 사용할 GitHub token 정보를 관리하는 순수 도메인 모델이다.
 */
public class GithubCredential {

    private static final int MIN_TOKEN_LENGTH_FOR_MASKING = 8;
    private static final int VISIBLE_PREFIX_LENGTH = 4;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;
    private static final String MASK_STRING = "****";

    private final Long id;
    private final Long workspaceId;
    private String displayName;
    private String token;
    private final String createdByGoogleSub;

    private GithubCredential(Long id, Long workspaceId, String displayName, String token, String createdByGoogleSub) {
        this.id = id;
        this.workspaceId = requireWorkspaceId(workspaceId);
        this.displayName = requireNotBlank(displayName, "displayName");
        this.token = requireNotBlank(token, "token");
        this.createdByGoogleSub = requireNotBlank(createdByGoogleSub, "createdByGoogleSub");
    }

    public static GithubCredential create(
            Long workspaceId, String displayName, String token, String createdByGoogleSub) {
        return new GithubCredential(null, workspaceId, displayName, token, createdByGoogleSub);
    }

    public static GithubCredential restore(
            Long id, Long workspaceId, String displayName, String token, String createdByGoogleSub) {
        return new GithubCredential(id, workspaceId, displayName, token, createdByGoogleSub);
    }

    public void update(String displayName, String token) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
        if (token != null && !token.isBlank()) {
            this.token = token.trim();
        }
    }

    public String maskedToken() {
        if (token.length() <= MIN_TOKEN_LENGTH_FOR_MASKING) {
            return MASK_STRING;
        }
        return token.substring(0, VISIBLE_PREFIX_LENGTH)
                + MASK_STRING
                + token.substring(token.length() - VISIBLE_SUFFIX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    private static Long requireWorkspaceId(Long workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return workspaceId;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
