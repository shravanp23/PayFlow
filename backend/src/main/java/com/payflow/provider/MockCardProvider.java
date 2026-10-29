package com.payflow.provider;

import org.springframework.stereotype.Component;

@Component
public class MockCardProvider extends AbstractMockProvider {

    @Override
    public String getName() {
        return "MockCardProvider";
    }

    @Override
    protected String referencePrefix() {
        return "CARD";
    }
}
