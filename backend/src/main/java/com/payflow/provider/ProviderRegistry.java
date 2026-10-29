package com.payflow.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<String, PaymentProvider> providersByName;

    public ProviderRegistry(List<PaymentProvider> providers) {
        this.providersByName = providers.stream()
            .collect(Collectors.toMap(PaymentProvider::getName, p -> p));
    }

    public PaymentProvider resolve(String providerName) {
        PaymentProvider provider = providersByName.get(providerName);
        if (provider == null) {
            // sensible default so demo payments without an explicit provider still work
            return providersByName.get("MockUPIProvider");
        }
        return provider;
    }
}
