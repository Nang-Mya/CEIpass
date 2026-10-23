package com.chiangraipass.config;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window limiter: at most {@code maxRequests} per key within {@code window}.
 * State is per application instance; with several instances the effective limit is
 * {@code maxRequests * instances}.
 */
public final class SlidingWindowRateLimiter {

    private static final int CLEANUP_EVERY = 1000;

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Limit and window must be positive.");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public Decision tryAcquire(String key) {
        long now = clock.millis();
        if (calls.incrementAndGet() % CLEANUP_EVERY == 0) {
            cleanup(now);
        }
        ArrayDeque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            prune(queue, now);
            if (queue.size() >= maxRequests) {
                long waitMillis = queue.peekFirst() + windowMillis - now;
                return new Decision(false, Math.max(1, (waitMillis + 999) / 1000));
            }
            queue.addLast(now);
            return new Decision(true, 0);
        }
    }

    int trackedKeys() {
        return hits.size();
    }

    private void prune(ArrayDeque<Long> queue, long now) {
        long cutoff = now - windowMillis;
        while (!queue.isEmpty() && queue.peekFirst() <= cutoff) {
            queue.pollFirst();
        }
    }

    private void cleanup(long now) {
        hits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }
}