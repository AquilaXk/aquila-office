package com.aquilaoffice.adapter.in.web.member;

/**
 * 멤버 프로필 수정 요청에서 전달되는 아바타 색상 값이다.
 */
public record MemberAvatarColorsRequest(String skin, String hair, String shirt) {}

