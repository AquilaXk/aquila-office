package com.aquilaoffice.application.port.out;

public interface ClaimSlackEventPort {

    boolean claimEvent(String eventId);
}
