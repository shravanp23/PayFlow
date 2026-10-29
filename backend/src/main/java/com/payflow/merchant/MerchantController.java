package com.payflow.merchant;

import com.payflow.merchant.dto.ApiKeyResponse;
import com.payflow.merchant.dto.CreateMerchantRequest;
import com.payflow.merchant.dto.MerchantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MerchantResponse> createMerchant(@Valid @RequestBody CreateMerchantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.createMerchant(request));
    }

    @GetMapping("/{merchantId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','VIEWER')")
    public MerchantResponse getMerchant(@PathVariable UUID merchantId) {
        return merchantService.getMerchant(merchantId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','VIEWER')")
    public List<MerchantResponse> listMerchants() {
        return merchantService.listMerchants();
    }

    @PostMapping("/{merchantId}/api-keys")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<ApiKeyResponse> generateApiKey(
        @PathVariable UUID merchantId,
        @RequestParam(defaultValue = "TEST") String environment
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.generateApiKey(merchantId, environment));
    }

    @PostMapping("/{merchantId}/api-keys/{keyId}/rotate")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ApiKeyResponse rotateApiKey(@PathVariable UUID merchantId, @PathVariable UUID keyId) {
        return merchantService.rotateApiKey(merchantId, keyId);
    }

    @DeleteMapping("/{merchantId}/api-keys/{keyId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(@PathVariable UUID merchantId, @PathVariable UUID keyId) {
        merchantService.revokeApiKey(merchantId, keyId);
    }

    @GetMapping("/{merchantId}/api-keys")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public List<ApiKeyResponse> listApiKeys(@PathVariable UUID merchantId) {
        return merchantService.listApiKeys(merchantId);
    }
}
