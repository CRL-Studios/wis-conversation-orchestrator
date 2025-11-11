package com.wis.orchestrator.service;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wis.orchestrator.entity.CustomerEntity;
import com.wis.orchestrator.entity.DevotionalPlanEntity;

import java.util.Optional;

/**
 * Service for accessing Cosmos DB.
 * Singleton pattern for use in Azure Functions.
 */
public class CosmosDBService {

    private static CosmosDBService instance;
    private final CosmosClient cosmosClient;
    private final CosmosDatabase database;
    private final ObjectMapper objectMapper;

    private static final String DATABASE_NAME = "WIS-Platform";
    private static final String PLANS_CONTAINER = "devotionalPlans";
    private static final String CUSTOMERS_CONTAINER = "customers";

    private CosmosDBService() {
        String uri = System.getenv("COSMOS_DB_URI");
        String key = System.getenv("COSMOS_DB_KEY");

        if (uri == null || key == null) {
            throw new IllegalStateException(
                    "COSMOS_DB_URI and COSMOS_DB_KEY environment variables must be set");
        }

        this.cosmosClient = new CosmosClientBuilder()
                .endpoint(uri)
                .key(key)
                .buildClient();

        this.database = cosmosClient.getDatabase(DATABASE_NAME);

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static synchronized CosmosDBService getInstance() {
        if (instance == null) {
            instance = new CosmosDBService();
        }
        return instance;
    }

    /**
     * Find a devotional plan by ID and customer ID.
     *
     * @param planId Plan ID
     * @param customerId Customer ID (partition key)
     * @return Optional containing the plan if found
     */
    public Optional<DevotionalPlanEntity> findPlanByIdAndCustomerId(String planId, String customerId) {
        try {
            CosmosContainer container = database.getContainer(PLANS_CONTAINER);

            String query = "SELECT * FROM c WHERE c.id = @planId AND c.customerId = @customerId";
            CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

            CosmosPagedIterable<DevotionalPlanEntity> results = container.queryItems(
                    query,
                    options,
                    DevotionalPlanEntity.class
            );

            return results.stream().findFirst();

        } catch (Exception e) {
            throw new RuntimeException("Failed to query devotional plan: " + e.getMessage(), e);
        }
    }

    /**
     * Find a customer by ID.
     *
     * @param customerId Customer ID
     * @return Optional containing the customer if found
     */
    public Optional<CustomerEntity> findCustomerById(String customerId) {
        try {
            CosmosContainer container = database.getContainer(CUSTOMERS_CONTAINER);

            CustomerEntity customer = container.readItem(
                    customerId,
                    new com.azure.cosmos.models.PartitionKey(customerId),
                    CustomerEntity.class
            ).getItem();

            return Optional.ofNullable(customer);

        } catch (com.azure.cosmos.CosmosException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new RuntimeException("Failed to query customer: " + e.getMessage(), e);
        }
    }

    /**
     * Update a customer's next plan message scheduled time.
     *
     * @param customer Customer entity to update
     */
    public void updateCustomer(CustomerEntity customer) {
        try {
            CosmosContainer container = database.getContainer(CUSTOMERS_CONTAINER);

            container.upsertItem(
                    customer,
                    new com.azure.cosmos.models.PartitionKey(customer.getId()),
                    new com.azure.cosmos.models.CosmosItemRequestOptions()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to update customer: " + e.getMessage(), e);
        }
    }
}
