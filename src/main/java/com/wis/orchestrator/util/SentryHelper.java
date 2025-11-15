package com.wis.orchestrator.util;

import io.sentry.Sentry;
import io.sentry.SentryOptions;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for Sentry error tracking and monitoring.
 * Provides initialization and exception capture helpers for Azure Functions.
 */
public class SentryHelper {

    private static final Logger logger = Logger.getLogger(SentryHelper.class.getName());
    private static boolean initialized = false;

    /**
     * Initialize Sentry with configuration from environment variables.
     * This should be called once when the function app starts.
     * Safe to call multiple times - will only initialize once.
     */
    public static synchronized void initializeSentry() {
        if (initialized) {
            return;
        }

        String dsn = System.getenv("SENTRY_DSN");
        String environment = System.getenv("AZURE_FUNCTIONS_ENVIRONMENT");

        if (dsn == null || dsn.isEmpty()) {
            logger.log(Level.WARNING, "SENTRY_DSN not configured - Sentry will not capture errors");
            initialized = true;
            return;
        }

        try {
            Sentry.init(options -> {
                options.setDsn(dsn);
                options.setEnvironment(environment != null ? environment : "production");
                options.setTracesSampleRate(1.0);
                options.setEnableTracing(true);
                options.setSendDefaultPii(false);
                options.setAttachStacktrace(true);
                options.setAttachThreads(true);
                options.setDebug(false);
                options.setMaxBreadcrumbs(100);
            });

            initialized = true;
            logger.log(Level.INFO, "Sentry initialized successfully for environment: {0}",
                new Object[]{environment != null ? environment : "production"});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize Sentry", e);
        }
    }

    /**
     * Capture an exception to Sentry.
     * Automatically initializes Sentry if not already done.
     *
     * @param exception The exception to capture
     */
    public static void captureException(Throwable exception) {
        ensureInitialized();

        try {
            Sentry.captureException(exception);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to capture exception to Sentry", e);
        }
    }

    /**
     * Capture a message to Sentry.
     * Useful for non-exception events that should be tracked.
     *
     * @param message The message to capture
     */
    public static void captureMessage(String message) {
        ensureInitialized();

        try {
            Sentry.captureMessage(message);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to capture message to Sentry", e);
        }
    }

    /**
     * Ensure Sentry is initialized before attempting to use it.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            initializeSentry();
        }
    }
}
