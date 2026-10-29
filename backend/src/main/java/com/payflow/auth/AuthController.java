package com.payflow.auth;

import com.payflow.auth.dto.LoginRequest;
import com.payflow.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${payflow.jwt.expiration-minutes}")
    private long expirationMinutes;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .filter(User::isActive)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginResponse(token, "Bearer", expirationMinutes, user.getEmail(), user.getRole().name());
    }

    // Stateless JWT: logout is a client-side token discard. If a server-side
    // revocation list is needed later, back it with a Redis blocklist keyed by jti.
    @PostMapping("/logout")
    public void logout() {
        // no-op for stateless JWT
    }
}
