package com.wis.orchestrator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.wis.orchestrator.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing CustomerEntity from Cosmos DB.
 */
@Repository
public interface CustomerRepository extends CosmosRepository<CustomerEntity, String> {
    // Standard CRUD operations provided by CosmosRepository
}
