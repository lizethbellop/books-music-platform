package com.musa.communities_service.polls.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Component
public class PollRequestLimits extends OncePerRequestFilter {

    private static final int MAX_BODY = 8192;

    private final HashMap<String, Bucket> buckets = new HashMap<>();

    private static class Bucket {
        double tokens = 60;
        long updated = System.nanoTime();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        boolean pollRoute = path.startsWith("/api/v1/polls/")
                || path.matches(
                "/api/v1/communities/[^/]+/polls(?:/.*)?"
        );

        return !pollRoute;
    }

    private synchronized boolean allow(String key) {
        long now = System.nanoTime();

        buckets.values().removeIf(
                bucket -> now - bucket.updated > 120_000_000_000L
        );

        Bucket bucket = buckets.get(key);

        if (bucket == null) {
            if (buckets.size() >= 2048) {
                return false;
            }

            bucket = new Bucket();
            buckets.put(key, bucket);
        }

        double elapsedSeconds =
                (now - bucket.updated) / 1_000_000_000.0;

        bucket.tokens = Math.min(
                60,
                bucket.tokens + elapsedSeconds * 2
        );

        bucket.updated = now;

        if (bucket.tokens < 1) {
            return false;
        }

        bucket.tokens--;
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String key = request.getUserPrincipal() == null
                ? request.getRemoteAddr()
                : request.getUserPrincipal().getName();

        if (!allow(key)) {
            response.setHeader("Retry-After", "5");
            fail(
                    response,
                    429,
                    "Demasiadas solicitudes. Espera unos segundos."
            );
            return;
        }

        if (!"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getContentLengthLong() > MAX_BODY) {
            fail(response, 413, "La solicitud es demasiado grande.");
            return;
        }

        byte[] body = request.getInputStream()
                .readNBytes(MAX_BODY + 1);

        if (body.length > MAX_BODY) {
            fail(response, 413, "La solicitud es demasiado grande.");
            return;
        }

        var wrapped = new HttpServletRequestWrapper(request) {

            @Override
            public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(body);

                return new ServletInputStream() {

                    @Override
                    public int read() {
                        return input.read();
                    }

                    @Override
                    public int read(byte[] bytes, int offset, int length) {
                        return input.read(bytes, offset, length);
                    }

                    @Override
                    public boolean isFinished() {
                        return input.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException(
                                "Solo lectura MVC síncrona"
                        );
                    }
                };
            }

            @Override
            public BufferedReader getReader() {
                return new BufferedReader(
                        new InputStreamReader(
                                getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
            }
        };

        chain.doFilter(wrapped, response);
    }

    private void fail(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}