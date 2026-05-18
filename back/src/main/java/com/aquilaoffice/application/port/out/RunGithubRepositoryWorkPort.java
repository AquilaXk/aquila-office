package com.aquilaoffice.application.port.out;

/**
 * 저장된 GitHub credential로 repository branch 작업과 PR 생성을 수행하는 outbound port다.
 */
public interface RunGithubRepositoryWorkPort {

    GithubRepositoryWorkResult run(GithubRepositoryWorkCommand command);
}
