# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build the project
mvn clean package

# Run locally with Azure Functions Core Tools
mvn azure-functions:run

# Test health check endpoint
curl http://localhost:7071/api/health

# Deploy to Azure
mvn azure-functions:deploy
```

## Architecture Overview

This is an **Azure Functions (Java 17)** application that orchestrates the Words in Season onboarding and messaging workflow. The application is event-driven and integrates with Azure Service Bus, Cosmos DB, and downstream messaging services.

### Event Flow

```
wis-registration
  └─> Publishes CustomerRegistered to topic: customer-events
       └─> ProcessCustomerRegistered function (tracks registration only)

wis-subscriptions
  └─> Publishes SubscriptionActivated to topic: subscription-events
       └─> ProcessSubscriptionActivated function
            ├─> Creates welcome message
            ├─> Queues to: message-send-queue
            └─> Initializes conversation state (TODO: Cosmos DB)

Timer (every 5 minutes)
  └─> MessageScheduler function
       ├─> Queries Cosmos DB for due messages
       └─> Queues devotionals/check-ins to: message-send-queue

Timer (every 5 minutes)
  └─> ProcessDevotionalPlanDay function
       ├─> Queries Cosmos DB for customers with active 7-day plans
       ├─> Loads plan from devotionalPlans container
       ├─> Queues current day's devotional to: message-send-queue
       └─> Includes metadata: { planId, dayNumber }

Timer (every 5 minutes)
  └─> ProcessPlanCompletion function
       ├─> Queries Cosmos DB for recently completed plans
       ├─> Sends weekly check-in prompt
       └─> User response triggers new plan or continues with themes

message-send-queue
  └─> Consumed by wis-message-handler
       ├─> Sends SMS via Twilio
       └─> Updates DevotionalPlanEntity (currentDay, status, completedAt)
```

### Key Architecture Patterns

1. **Separation of Concerns**:
   - **Orchestrator**: Scheduling logic - detects WHEN things need to happen, queues requests
   - **Message-handler**: Execution logic - loads data, formats messages, sends SMS

2. **Welcome Message Trigger**: Originally triggered on `CustomerRegistered`, now triggers on `SubscriptionActivated` (after payment). The `ProcessCustomerRegistered` function currently only logs registration for tracking.

3. **Message Format**:
   - **Complete messages** (welcome, plan devotionals): Include `phoneNumber` and `message` fields
   - **Request messages** (weekly check-in): Only include `customerId` and `messageType`; message-handler loads and formats

4. **Priority Field**: MUST use uppercase values (`"HIGH"`, `"NORMAL"`) to match wis-message-handler format expectations.

5. **Message Preprocessing**: Message-handler's `preprocessMessage()` enriches request-only messages before sending.

6. **Conversation State**: Uses `ConversationService` to initialize state in Cosmos DB (currently stubbed out - see TODO comments in [ConversationService.java:28-38](src/main/java/com/wis/orchestrator/service/ConversationService.java#L28-L38)).

## Azure Functions

### ProcessSubscriptionActivated
- **Trigger**: Service Bus topic `subscription-events` (subscription: `orchestration-subscription`)
- **Purpose**: Sends welcome message after subscription activation
- **Output**: Queues to `message-send-queue`
- **File**: [SubscriptionActivatedFunction.java:40](src/main/java/com/wis/orchestrator/SubscriptionActivatedFunction.java#L40)

### ProcessCustomerRegistered
- **Trigger**: Service Bus topic `customer-events` (subscription: `orchestration-subscription`)
- **Purpose**: Tracks registration events (welcome message now sent after subscription activation)
- **File**: [ConversationOrchestratorFunction.java:40](src/main/java/com/wis/orchestrator/ConversationOrchestratorFunction.java#L40)

### MessageScheduler
- **Trigger**: Timer (every 5 minutes via cron: `0 */5 * * * *`)
- **Purpose**: Queries Cosmos DB for customers with scheduled devotionals or check-ins
- **Output**: Queues messages to `message-send-queue`
- **File**: [SchedulerFunction.java:42](src/main/java/com/wis/orchestrator/SchedulerFunction.java#L42)

### ProcessDevotionalPlanDay
- **Trigger**: Timer (every 5 minutes via cron: `0 */5 * * * *`)
- **Purpose**: Sends daily devotional messages from active 7-day plans
- **Query**: Customers with `activePlanId != null` AND `nextPlanMessageScheduledFor <= now()`
- **Output**: Queues messages with metadata `{ planId, dayNumber, messageType: "daily_plan_devotion" }`
- **File**: [DevotionalPlanFunction.java:36](src/main/java/com/wis/orchestrator/DevotionalPlanFunction.java#L36)

### ProcessPlanCompletion
- **Trigger**: Timer (every 5 minutes via cron: `0 */5 * * * *`)
- **Purpose**: Sends weekly check-in prompt after 7-day plan completes
- **Query**: Plans with `status = 'completed'` AND `completedAt >= now() - 24h` AND `!checkInSent`
- **Output**: Queues weekly check-in message asking user to update their season
- **File**: [DevotionalPlanFunction.java:165](src/main/java/com/wis/orchestrator/DevotionalPlanFunction.java#L165)

### HealthCheck
- **Trigger**: HTTP GET `/api/health`
- **Auth Level**: Anonymous
- **File**: [ConversationOrchestratorFunction.java:110](src/main/java/com/wis/orchestrator/ConversationOrchestratorFunction.java#L110)

## 7-Day Devotional Plan Lifecycle

The core user experience is a 7-day devotional plan that continuously cycles based on the user's life season.

### Plan Creation
1. User shares their life season (via SMS to message-handler)
2. Message-handler extracts themes and generates 7-day plan
3. Plan stored in `devotionalPlans` container with status `"active"`
4. Customer's `activePlanId` field set to plan ID

### Daily Message Delivery
1. `ProcessDevotionalPlanDay` runs every 5 minutes
2. Queries customers with `activePlanId` where next message is due
3. Loads `DevotionalPlanEntity` from Cosmos DB
4. Queues message with metadata: `{ planId, dayNumber, messageType: "daily_plan_devotion" }`
5. Message-handler sends SMS and updates plan's `currentDay` field

### Plan Completion (Day 7)
1. Message-handler detects `dayNumber == 7` in metadata
2. Updates plan: `status = "completed"`, sets `completedAt` timestamp
3. Clears customer's `activePlanId` field
4. Plan remains in database for audit/history

### Weekly Check-In
1. `ProcessPlanCompletion` runs every 5 minutes
2. Finds plans completed in last 24 hours without check-in sent
3. Queues check-in request with `messageType: "weekly_check_in"` (no message/phoneNumber)
4. Message-handler's `preprocessMessage()`:
   - Loads customer data (firstName, phoneNumber)
   - Formats message: "Hey [FirstName]! Just checking in for your weekly update 💬..."
   - Marks plan's `checkInSent = true`
   - Sends SMS
5. User can respond with:
   - **New season description** → Message-handler creates new 7-day plan
   - **"no change"** → System continues with regular devotionals using existing themes

### Continuous Cycle
This creates a weekly rhythm:
- **Days 1-7**: Structured devotional plan
- **Day 8**: Weekly check-in prompt
- **Repeat**: New plan or continue with themes

## Data Models

### Event Models
- `CustomerRegisteredEvent`: From wis-registration service (topic: customer-events)
- `SubscriptionActivatedEvent`: From wis-subscriptions service (topic: subscription-events)

### Message Models
- `WelcomeMessage`: Sent to message-send-queue for SMS delivery
- `ScheduledMessageRequest`: Used for devotional and check-in messages
- `DevotionalPlanMessage`: 7-day plan message with planId and dayNumber metadata
- `WeeklyCheckInMessage`: Check-in prompt after plan completion

## Service Bus Configuration

Required topics and subscriptions:
- **Topic**: `customer-events` → Subscription: `orchestration-subscription`
- **Topic**: `subscription-events` → Subscription: `orchestration-subscription`
- **Queue**: `message-send-queue` (consumed by wis-message-handler)

Connection managed via `ServiceBusConnection` environment variable.

## Cosmos DB Integration

The application uses Cosmos DB for:
- Conversation state tracking (database: `WIS-Platform`, container: `customers`)
- 7-day devotional plans (database: `WIS-Platform`, container: `devotionalPlans`)
- MessageScheduler queries customers with fields:
  - `messagingState.nextDevotionalScheduledFor`
  - `messagingState.nextCheckInScheduledFor`
  - `messagingState.conversationState`

### Required CustomerEntity Fields

For 7-day plan orchestration, customers need:
```java
String activePlanId;  // Current plan ID, null if no active plan
messagingState: {
    Instant nextPlanMessageScheduledFor;  // When to send next plan day
    String timezone;  // e.g., "America/New_York"
    String preferredTimeOfDay;  // e.g., "08:00"
}
```

### DevotionalPlanEntity Structure

Plans are managed by message-handler but queried by orchestrator:
- `id`: Unique plan ID (partition key: `customerId`)
- `customerId`: Owner of this plan
- `status`: "active", "completed", "cancelled"
- `currentDay`: 1-7 tracking progress
- `completedAt`: Timestamp when Day 7 sent
- `checkInSent`: Boolean flag to prevent duplicate check-ins
- `days[]`: Array of 7 DailyDevotion objects with content

### Message Metadata Requirements

All 7-day plan messages MUST include metadata:
```json
{
  "planId": "plan-uuid",
  "dayNumber": 3,
  "messageType": "daily_plan_devotion"
}
```

This metadata is critical - message-handler's ServiceBusListener checks these fields to update plan progress.

**Note**: ConversationService Cosmos DB implementation is currently stubbed (see [ConversationService.java:28-38](src/main/java/com/wis/orchestrator/service/ConversationService.java#L28-L38)).

## Environment Variables

| Variable | Purpose |
|----------|---------|
| `ServiceBusConnection` | Azure Service Bus connection string |
| `COSMOS_DB_URI` | Cosmos DB endpoint URI (e.g., https://account.documents.azure.com:443/) |
| `COSMOS_DB_KEY` | Cosmos DB primary or secondary key |
| `CosmosDBConnection` | Alternative connection string format used by Cosmos DB input bindings |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | Application Insights for monitoring |

Configure in `local.settings.json` for local development (not checked into git).

### Cosmos DB Access

The orchestrator uses a singleton `CosmosDBService` for Cosmos DB access:
- **Database**: `WIS-Platform`
- **Containers**: `devotionalPlans`, `customers`
- **Read-only access**: Orchestrator queries plans and customers, but updates are minimal (nextPlanMessageScheduledFor)
- **Partition keys**: Plans use `customerId`, customers use `id`
- **Implementation**: [CosmosDBService.java](src/main/java/com/wis/orchestrator/service/CosmosDBService.java) (singleton pattern)

## Important Implementation Details

1. **Logger Signature**: Use `logger.log(Level.X, "message with {0} placeholders", new Object[]{param1, param2})` - parameters must be wrapped in Object array (see [ConversationOrchestratorFunction.java:57-58](src/main/java/com/wis/orchestrator/ConversationOrchestratorFunction.java#L57-L58)).

2. **Field Naming**: Message field names must match wis-message-handler expectations:
   - Use `phoneNumber` (not `phone`)
   - Use uppercase priority values: `"HIGH"`, `"NORMAL"` (not lowercase)

3. **Jackson Configuration**: ObjectMapper requires `JavaTimeModule` for Java 8 time types (Instant, etc.).

4. **Retry Logic**: Service Bus automatic retries configured (max 5 attempts for topics, 3 for queues). Functions should throw RuntimeException to trigger retries.

5. **Cosmos DB SQL Query Syntax**: Uses Cosmos DB SQL syntax in `@CosmosDBInput` annotations:
   - `GetCurrentDateTime()` for current timestamp
   - `DateTimeAdd()` for date arithmetic
   - `IS_DEFINED()` for null checks

## Related Services

- **wis-registration**: Publishes CustomerRegistered events
- **wis-subscriptions**: Publishes SubscriptionActivated events
- **wis-message-handler**: Consumes message-send-queue and sends SMS via Twilio
