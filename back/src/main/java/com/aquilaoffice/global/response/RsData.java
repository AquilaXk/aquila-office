package com.aquilaoffice.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RsData<T>(boolean success, String code, String message, T data) {

    public static <T> RsData<T> ok(T data) {
        return new RsData<>(true, "200", "OK", data);
    }

    public static <T> RsData<T> fail(String code, String message) {
        return new RsData<>(false, code, message, null);
    }
}
