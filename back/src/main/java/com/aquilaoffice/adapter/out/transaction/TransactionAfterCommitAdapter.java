package com.aquilaoffice.adapter.out.transaction;

import com.aquilaoffice.application.port.out.RunAfterCommitPort;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring transaction이 활성화된 경우 callback을 commit 이후로 지연 실행한다.
 */
@Component
public class TransactionAfterCommitAdapter implements RunAfterCommitPort {

    @Override
    public void runAfterCommit(Runnable callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            callback.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callback.run();
            }
        });
    }
}
