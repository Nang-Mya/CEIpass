package com.chiangraipass.config;

public final class OAuthPrivacyNoticeContext {

    public static final String SESSION_KEY = "oauth-privacy-notice-version";
    private static final ThreadLocal<String> NOTICE_VERSION = new ThreadLocal<>();

    private OAuthPrivacyNoticeContext() {
    }

    public static String currentNoticeVersion() {
        return NOTICE_VERSION.get();
    }

    public static Scope activate(String noticeVersion) {
        NOTICE_VERSION.set(noticeVersion);
        return NOTICE_VERSION::remove;
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
