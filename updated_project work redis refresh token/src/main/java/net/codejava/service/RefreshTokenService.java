package net.codejava.service;

import net.codejava.model.AppUser;
import net.codejava.model.RefreshToken;
import net.codejava.repository.AppUserRepository;
import net.codejava.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository tokenRepository;
    private final AppUserRepository userRepository;


    private final long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository tokenRepository,
                               AppUserRepository userRepository,
                               @Value("${app.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }


    public RefreshToken createRefreshToken(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find user: " + username));

        tokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshExpirationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        return tokenRepository.save(refreshToken);
    }


    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }


    public boolean verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().before(new Date())) {
            tokenRepository.delete(token);
            return false;
        }
        return true;
    }


    public void deleteTokensByUser(AppUser user) {
        tokenRepository.deleteByUser(user);
    }


    public void deleteByToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }
}