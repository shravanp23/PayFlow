package com.payflow.provider;

import org.springframework.stereotype.Component;

@Component
public class MockUPIProvider extends AbstractMockProvider {

    @Override
    public String getName() {
        return "MockUPIProvider";
    }

    @Override
    protected String referencePrefix() {
        return "UPI";
    }
}
