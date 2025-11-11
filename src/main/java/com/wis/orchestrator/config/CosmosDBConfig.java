package com.wis.orchestrator.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Azure Cosmos DB connection and repositories.
 */
@Configuration
@EnableCosmosRepositories(basePackages = "com.wis.orchestrator.repository")
public class CosmosDBConfig extends AbstractCosmosConfiguration {

    private static final String DATABASE_NAME = "WIS-Platform";

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        String uri = System.getenv("COSMOS_DB_URI");
        String key = System.getenv("COSMOS_DB_KEY");

        if (uri == null || key == null) {
            throw new IllegalStateException(
                    "COSMOS_DB_URI and COSMOS_DB_KEY environment variables must be set");
        }

        return new CosmosClientBuilder()
                .endpoint(uri)
                .key(key);
    }

    @Bean
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(false)
                .build();
    }
}
