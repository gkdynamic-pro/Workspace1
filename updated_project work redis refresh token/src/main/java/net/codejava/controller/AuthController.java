package net.codejava.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.codejava.util.JwtUtil;
import net.codejava.service.TokenBlacklistService;
import net.codejava.service.RefreshTokenService;
import net.codejava.model.AppUser;
import net.codejava.model.RefreshToken;
import net.codejava.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    private final AppUserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          PasswordEncoder encoder,
                          AppUserRepository userRepository,
                          TokenBlacklistService tokenBlacklistService,
                          RefreshTokenService refreshTokenService){
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request,
                                                     jakarta.servlet.http.HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = jwtUtil.generateToken(request.getUsername());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        long maxAgeSeconds = Math.max(1L,
                (refreshToken.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000);
        cookie.setMaxAge((int) maxAgeSeconds);
        response.addCookie(cookie);
        return ResponseEntity.ok(new AuthResponse(accessToken));
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new Message("Username already taken"));
        }
        Set<String> roles = request.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("USER");
        }
        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword())); // BCrypt hash
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new Message("User registered successfully"));
    }


    @Data
    static class AuthRequest {
        private String username;
        private String password;
    }

    @Data
    @AllArgsConstructor
    static class AuthResponse {
        private String jwt;
    }

    @Data
    static class SignupRequest {
        private String username;
        private String password;
        private Set<String> roles;
    }

    @Data
    @AllArgsConstructor
    static class Message {
        private String message;
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new Message("Missing Authorization header"));
        }
        String token = auth.substring(7);


        String jti = jwtUtil.extractJti(token);
        java.util.Date exp = jwtUtil.getExpirationDate(token);
        long ttlSeconds = Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);

        String key = "bl:access:" + (jti != null ? jti : Integer.toHexString(token.hashCode()));
        tokenBlacklistService.blacklist(key, ttlSeconds);


        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("refreshToken".equals(c.getName())) {
                    String refreshTokenValue = c.getValue();
                    refreshTokenService.deleteByToken(refreshTokenValue);

                    jakarta.servlet.http.Cookie deleteCookie = new jakarta.servlet.http.Cookie("refreshToken", "");
                    deleteCookie.setHttpOnly(true);
                    deleteCookie.setSecure(false);
                    deleteCookie.setPath("/");
                    deleteCookie.setMaxAge(0);
                    response.addCookie(deleteCookie);
                    break;
                }
            }
        }
        return ResponseEntity.ok(new Message("Logged out"));
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(403).build();
        }
        for (jakarta.servlet.http.Cookie c : cookies) {
            if ("refreshToken".equals(c.getName())) {
                String refreshTokenValue = c.getValue();
                java.util.Optional<RefreshToken> opt = refreshTokenService.findByToken(refreshTokenValue);
                if (opt.isEmpty()) {
                    return ResponseEntity.status(403).build();
                }
                RefreshToken rt = opt.get();
                if (!refreshTokenService.verifyExpiration(rt)) {
                    return ResponseEntity.status(403).build();
                }
                String username = rt.getUser().getUsername();
                String newAccessToken = jwtUtil.generateToken(username);

                RefreshToken newRt = refreshTokenService.createRefreshToken(username);
                jakarta.servlet.http.Cookie newCookie = new jakarta.servlet.http.Cookie("refreshToken", newRt.getToken());
                newCookie.setHttpOnly(true);
                newCookie.setSecure(false);
                newCookie.setPath("/");
                long newMaxAge = Math.max(1L,
                        (newRt.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000);
                newCookie.setMaxAge((int) newMaxAge);
                response.addCookie(newCookie);
                return ResponseEntity.ok(new AuthResponse(newAccessToken));
            }
        }
        return ResponseEntity.status(403).build();
    }

}
