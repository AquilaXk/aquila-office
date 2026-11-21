package com.aquilaoffice.application.port.out;

/**
 * application service가 transaction commit 이후 실행할 side effect를 예약하기 위한 port다.
 */
public interface RunAfterCommitPort {

    void runAfterCommit(Runnable callback);

    static RunAfterCommitPort immediate() {
        return Runnable::run;
    }
}
