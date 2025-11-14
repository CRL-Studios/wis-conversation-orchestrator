package com.wis.orchestrator.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Singleton Spring ApplicationContext for Azure Functions.
 * Provides access to Spring beans in a non-Spring environment.
 */
public class SpringContext {

    private static ApplicationContext context;

    private SpringContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get or create the Spring ApplicationContext.
     * This initializes the Cosmos DB configuration and repositories.
     *
     * @return The Spring ApplicationContext
     */
    public static synchronized ApplicationContext getContext() {
        if (context == null) {
            AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
            ctx.register(CosmosDBConfig.class);
            ctx.refresh();
            context = ctx;
        }
        return context;
    }

    /**
     * Get a bean from the Spring context.
     *
     * @param beanClass The class of the bean to retrieve
     * @param <T> The type of the bean
     * @return The bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        return getContext().getBean(beanClass);
    }
}
