package com.cypay.framework.security;

import com.cypay.framework.acteur.ActeurJwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Filtre JWT réutilisable pour tous les microservices
 */
public class ActeurJwtAuthenticationFilter extends OncePerRequestFilter {

    private final ActeurJwtValidator jwtActeur;

    public ActeurJwtAuthenticationFilter(String secret, long expiration) {
        this.jwtActeur = new ActeurJwtValidator("JwtAuthFilter", secret, expiration);
    }

    public ActeurJwtAuthenticationFilter(ActeurJwtValidator jwtActeur) {
        this.jwtActeur = jwtActeur;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if (authorizationHeader != null) {
            jwt = jwtActeur.extraireTokenDepuisHeader(authorizationHeader);

            if (jwt != null && jwtActeur.validerToken(jwt)) {
                email = jwtActeur.extraireEmail(jwt);
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    public ActeurJwtValidator getJwtActeur() {
        return jwtActeur;
    }
}