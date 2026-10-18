package com.workloadgovernance.security;

import tools.jackson.databind.json.JsonMapper;
import com.workloadgovernance.controller.ApiExceptionHandler.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Component
public class TenantApiKeyFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_ATTRIBUTE =
            "authenticatedTenantId";

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public TenantApiKeyFilter(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey == null || apiKey.isBlank()) {
            reject(request, response);
            return;
        }

        List<String> tenantIds = jdbcTemplate.queryForList(
                """
                SELECT public.resolve_tenant_for_api_key(?)
                """,
                String.class,
                hash(apiKey)
        );

        if (tenantIds.isEmpty()) {
            reject(request, response);
            return;
        }

        request.setAttribute(
                TENANT_ID_ATTRIBUTE,
                tenantIds.get(0)
        );

        filterChain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        jsonMapper.writeValue(
                response.getOutputStream(),
                new ApiError(
                        Instant.now(),
                        401,
                        "Unauthorized",
                        "Missing or invalid API key",
                        request.getRequestURI()
                )
        );
    }

    private static String hash(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(
                            apiKey.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}

