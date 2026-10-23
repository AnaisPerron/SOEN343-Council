package com.council.tenderapp.reg.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.council.tenderapp.reg.services.SessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


//class method made to run once per http request and call session service to validate session
public class TokenAuthenticationFilter extends OncePerRequestFilter 
{
    private final SessionService sessionService;

    public TokenAuthenticationFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override 
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            Optional<Session> sessionOpt = sessionService.find(token); //uses session service to verufy token
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();

                List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + session.role().name())
                );

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(session.accountId(), null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }
}
