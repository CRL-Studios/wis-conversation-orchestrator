# WIS Conversation Orchestrator - Azure Functions

Azure Functions application that orchestrates the Words in Season onboarding workflow. Processes `CustomerRegistered` events from Service Bus and initiates welcome messages.

## Overview

This function app:
1. **Listens** to `customer-events` Service Bus topic (via `orchestration-subscription`)
2. **Processes** `CustomerRegistered` events published by `wis-registration` service
3. **Queues** welcome messages to `message-send-queue` for SMS delivery
4. **Initializes** conversation state in Cosmos DB for tracking user interactions

## Architecture

```
wis-registration
  └─> Publishes CustomerRegistered event to Service Bus topic: customer-events
       └─> Subscription: orchestration-subscription
            └─> This Function: ProcessCustomerRegistered
                 ├─> Creates welcome message
                 ├─> Sends to queue: message-send-queue
                 └─> Initializes conversation state in Cosmos DB

message-send-queue
  └─> Consumed by wis-message-handler
       └─> Sends SMS via Twilio
```

## Prerequisites

- Java 17
- Maven 3.8+
- Azure Functions Core Tools v4
- Azure CLI (for deployment)
- Azure Service Bus namespace with:
  - Topic: `customer-events`
  - Subscription: `orchestration-subscription`
  - Queue: `message-send-queue`

## Local Development

### Setup

1. **Configure Azure Service Bus**

Create topic and subscription:
```bash
az servicebus topic create \
  --resource-group wis-platform-prod \
  --namespace-name wis-platform-servicebus \
  --name customer-events

az servicebus topic subscription create \
  --resource-group wis-platform-prod \
  --namespace-name wis-platform-servicebus \
  --topic-name customer-events \
  --name orchestration-subscription \
  --max-delivery-count 5
```

Create message queue:
```bash
az servicebus queue create \
  --resource-group wis-platform-prod \
  --namespace-name wis-platform-servicebus \
  --name message-send-queue \
  --max-delivery-count 3
```

2. **Update local.settings.json**

Get connection string:
```bash
az servicebus namespace authorization-rule keys list \
  --resource-group wis-platform-prod \
  --namespace-name wis-platform-servicebus \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString -o tsv
```

Update `local.settings.json`:
```json
{
  "Values": {
    "ServiceBusConnection": "Endpoint=sb://wis-platform-servicebus.servicebus.windows.net/;..."
  }
}
```

### Build & Run

```bash
# Build the project
mvn clean package

# Run locally with Azure Functions Core Tools
mvn azure-functions:run

# Test health check
curl http://localhost:7071/api/health
```

### Testing

**Send a test CustomerRegistered event:**

```bash
# Using Azure CLI
az servicebus topic message send \
  --resource-group wis-platform-prod \
  --namespace-name wis-platform-servicebus \
  --topic-name customer-events \
  --body '{
    "eventId": "test-123",
    "eventType": "CustomerRegistered",
    "eventTime": "2025-10-29T12:00:00Z",
    "subject": "customers/test-customer-id",
    "data": {
      "customerId": "test-customer-id",
      "phone": "+15551234567",
      "registrationStage": "phone_verified",
      "createdAt": "2025-10-29T12:00:00Z"
    }
  }'
```

**Check function logs:**
```bash
# Logs will appear in the Azure Functions runtime console
```

## Deployment

### Deploy to Azure

```bash
# Login to Azure
az login

# Create Function App (if not exists)
az functionapp create \
  --resource-group wis-platform-prod \
  --consumption-plan-location eastus \
  --runtime java \
  --runtime-version 17 \
  --functions-version 4 \
  --name wis-conversation-orchestrator \
  --storage-account wisfunctionstorage

# Deploy
mvn azure-functions:deploy
```

### Configure Application Settings

```bash
# Set Service Bus connection string
az functionapp config appsettings set \
  --name wis-conversation-orchestrator \
  --resource-group wis-platform-prod \
  --settings "ServiceBusConnection=@Microsoft.KeyVault(SecretUri=https://wis-keyvault.vault.azure.net/secrets/ServiceBusConnection)"
```

## Functions

### ProcessCustomerRegistered

**Trigger:** Service Bus Topic (`customer-events`, subscription: `orchestration-subscription`)

**Purpose:** Processes new customer registrations and queues welcome messages

**Output:** Sends message to `message-send-queue` for SMS delivery

**Retry Policy:** Automatic retries via Service Bus (max 5 attempts)

### HealthCheck

**Trigger:** HTTP GET `/api/health`

**Purpose:** Health check endpoint for monitoring

**Auth Level:** Anonymous

## Monitoring

**Application Insights Queries:**

```kusto
// Function execution count
traces
| where message contains "ProcessCustomerRegistered"
| summarize count() by bin(timestamp, 5m)

// Failed executions
exceptions
| where outerMessage contains "ProcessCustomerRegistered"
| project timestamp, outerMessage, innerMessage

// Welcome messages queued
traces
| where message contains "Welcome message queued successfully"
| summarize count() by bin(timestamp, 1h)
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `ServiceBusConnection` | Service Bus connection string | Yes |
| `CosmosDBConnection` | Cosmos DB connection string | Future |
| `SENTRY_DSN` | Sentry error tracking DSN | Optional |

## Welcome Message Text

The current welcome message asks:

> "Welcome to Words in Season! We're here to walk with you through life's seasons.
>
> Tell us: What season of life are you in right now? (For example: facing a challenge, celebrating a victory, seeking direction, etc.)"

This can be customized in `ConversationOrchestratorFunction.buildWelcomeMessageText()`.

## Troubleshooting

**Function not triggering:**
- Verify Service Bus subscription filter (should allow `eventType = 'CustomerRegistered'`)
- Check Service Bus connection string in app settings
- Ensure topic has messages (check Azure Portal)

**Messages not being queued:**
- Check `message-send-queue` exists
- Verify output binding connection string
- Check function logs for serialization errors

**High latency:**
- Check Service Bus throttling limits
- Review Application Insights performance metrics
- Consider increasing Function App hosting plan

## Next Steps

1. Implement Cosmos DB conversation state tracking
2. Add retry logic for failed message queueing
3. Create follow-up functions for conversation management
4. Add AI integration for personalized responses

## Related Services

- **wis-registration**: Publishes CustomerRegistered events
- **wis-message-handler**: Consumes message-send-queue and sends SMS
- **wis-subscriptions**: Manages subscription status