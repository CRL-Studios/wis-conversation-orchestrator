package com.wis.orchestrator.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.wis.orchestrator.entity.DevotionalPlanEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing DevotionalPlanEntity from Cosmos DB.
 */
@Repository
public interface DevotionalPlanRepository extends CosmosRepository<DevotionalPlanEntity, String> {

    /**
     * Find a plan by ID and customer ID (partition key).
     *
     * @param id Plan ID
     * @param customerId Customer ID (partition key)
     * @return Optional containing the plan if found
     */
    Optional<DevotionalPlanEntity> findByIdAndCustomerId(String id, String customerId);
}
