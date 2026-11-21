package com.aquilaoffice.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 성공/실패 응답이 공유하는 공통 envelope다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RsData<T>(boolean success, String code, String message, T data) {

    public static <T> RsData<T> ok(T data) {
        return new RsData<>(true, "200", "OK", data);
    }

    public static <T> RsData<T> fail(String code, String message) {
        return new RsData<>(false, code, message, null);
    }
}
