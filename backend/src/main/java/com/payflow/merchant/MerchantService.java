package com.payflow.merchant;

import com.payflow.merchant.dto.ApiKeyResponse;
import com.payflow.merchant.dto.CreateMerchantRequest;
import com.payflow.merchant.dto.MerchantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        Merchant merchant = Merchant.create(request.name(), request.email());
        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
            .map(MerchantResponse::from)
            .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> listMerchants() {
        return merchantRepository.findAll().stream().map(MerchantResponse::from).toList();
    }

    /** Generates a new API key. Returns the raw key exactly once — it cannot be retrieved again. */
    @Transactional
    public ApiKeyResponse generateApiKey(UUID merchantId, String environment) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new MerchantNotFoundException(merchantId);
        }
        var generated = apiKeyGenerator.generate(environment);
        ApiKey apiKey = ApiKey.create(merchantId, generated.hash(), generated.prefix(), environment.toUpperCase());
        apiKeyRepository.save(apiKey);
        return new ApiKeyResponse(apiKey.getId(), apiKey.getEnvironment(), apiKey.getKeyPrefix(),
            generated.rawKey(), false, apiKey.getCreatedAt());
    }

    /** Revokes the old key and issues a new one in a single operation. */
    @Transactional
    public ApiKeyResponse rotateApiKey(UUID merchantId, UUID oldKeyId) {
        ApiKey oldKey = apiKeyRepository.findById(oldKeyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + oldKeyId));
        if (!oldKey.getMerchantId().equals(merchantId)) {
            throw new IllegalArgumentException("API key does not belong to merchant");
        }
        oldKey.revoke();
        apiKeyRepository.save(oldKey);
        return generateApiKey(merchantId, oldKey.getEnvironment());
    }

    @Transactional
    public void revokeApiKey(UUID merchantId, UUID keyId) {
        ApiKey key = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
        if (!key.getMerchantId().equals(merchantId)) {
            throw new IllegalArgumentException("API key does not belong to merchant");
        }
        key.revoke();
        apiKeyRepository.save(key);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchantId(merchantId).stream()
            .map(k -> new ApiKeyResponse(k.getId(), k.getEnvironment(), k.getKeyPrefix(), null, k.isRevoked(), k.getCreatedAt()))
            .toList();
    }
}
