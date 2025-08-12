package net.codejava.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.codejava.service.TokenBlacklistService;
import net.codejava.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;
    private final UserDetailsService uds;
    private final TokenBlacklistService blacklist;

    public JwtFilter(JwtUtil jwt, UserDetailsService uds, TokenBlacklistService blacklist) {
        this.jwt = jwt;
        this.uds = uds;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);

            String jti = jwt.extractJti(token);
            String key = "bl:access:" + (jti != null ? jti : Integer.toHexString(token.hashCode()));

            if (!blacklist.isBlacklisted(key) && jwt.validateToken(token)) {
                String username = jwt.extractUsername(token);
                UserDetails user = uds.loadUserByUsername(username);

                var authToken = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(req, res);
    }
}
