# Supabase Realtime Setup Guide

## Overview

This Spring Boot application is configured to listen to real-time changes from your Supabase database using WebSocket connections.

## Configuration

### 1. Update `application.yml`

Replace `your-anon-key-here` with your actual Supabase anonymous key:

```yaml
supabase:
  project-url: https://oywnkekqmfhgioblvbdg.supabase.co
  anon-key: your-actual-anon-key  # Get this from Supabase Dashboard
  realtime:
    enabled: true
    tables:
      - messages
      - rooms
      - clusters
```

### 2. Get Your Supabase Anon Key

1. Go to your Supabase Dashboard: https://app.supabase.com
2. Select your project
3. Go to Settings → API
4. Copy the `anon` key (public)

### 3. Enable Realtime in Supabase

For each table you want to listen to, you need to enable Realtime:

1. Go to your Supabase Dashboard
2. Navigate to Database → Replication
3. Enable replication for the tables: `messages`, `rooms`, `clusters`

Or run this SQL in the Supabase SQL Editor:

```sql
-- Enable realtime for messages table
ALTER TABLE public.messages REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE messages;

-- Enable realtime for rooms table
ALTER TABLE public.rooms REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE rooms;

-- Enable realtime for clusters table
ALTER TABLE public.clusters REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE clusters;
```

## How It Works

### Architecture

```
Supabase Database → WebSocket → SupabaseRealtimeService → Listeners
```

1. **SupabaseRealtimeService**: Manages WebSocket connection and subscriptions
2. **RealtimeChangeListener**: Interface for handling database changes
3. **MessageRealtimeListener**: Example implementation for handling message changes

### Components

#### 1. SupabaseRealtimeService

The main service that:
- Establishes WebSocket connection to Supabase
- Subscribes to configured tables
- Parses incoming events
- Notifies registered listeners
- Auto-reconnects on connection loss

#### 2. RealtimeChangeListener Interface

```java
public interface RealtimeChangeListener {
    void onInsert(String table, Map<String, Object> record);
    void onUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord);
    void onDelete(String table, Map<String, Object> oldRecord);
}
```

#### 3. MessageRealtimeListener (Example)

An example implementation that logs message changes. You can extend this to:
- Send WebSocket notifications to connected clients
- Update caches
- Trigger business logic
- Send notifications

## Creating Your Own Listeners

### Example: Room Changes Listener

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class RoomRealtimeListener implements RealtimeChangeListener {
    
    private final SupabaseRealtimeService realtimeService;
    
    @PostConstruct
    public void init() {
        realtimeService.addListener(this);
    }
    
    @Override
    public void onInsert(String table, Map<String, Object> record) {
        if ("rooms".equals(table)) {
            String roomId = String.valueOf(record.get("id"));
            String roomName = String.valueOf(record.get("name"));
            log.info("New room created: {} ({})", roomName, roomId);
            
            // Your business logic here
        }
    }
    
    @Override
    public void onUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord) {
        if ("rooms".equals(table)) {
            // Handle room updates
        }
    }
    
    @Override
    public void onDelete(String table, Map<String, Object> oldRecord) {
        if ("rooms".equals(table)) {
            // Handle room deletion
        }
    }
}
```

## Common Use Cases

### 1. Real-time Notifications

```java
@Override
public void onInsert(String table, Map<String, Object> record) {
    if ("messages".equals(table)) {
        String roomId = String.valueOf(record.get("room_id"));
        
        // Send notification to all users in the room
        webSocketService.sendToRoom(roomId, "NEW_MESSAGE", record);
    }
}
```

### 2. Cache Invalidation

```java
@Override
public void onUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord) {
    if ("messages".equals(table)) {
        String messageId = String.valueOf(newRecord.get("id"));
        
        // Invalidate cache
        cacheManager.evict("messages", messageId);
    }
}
```

### 3. Trigger ML/Clustering

```java
@Override
public void onInsert(String table, Map<String, Object> record) {
    if ("messages".equals(table)) {
        String content = String.valueOf(record.get("content"));
        String roomId = String.valueOf(record.get("room_id"));
        
        // Trigger clustering algorithm
        clusteringService.analyzeAndCluster(roomId, content);
    }
}
```

## Testing

### 1. Start the application

```bash
./gradlew bootRun
```

### 2. Check logs

You should see:
```
Connecting to Supabase Realtime: wss://...
WebSocket connection opened
Subscribed to table: messages
Subscribed to table: rooms
Subscribed to table: clusters
```

### 3. Test with Supabase Dashboard

1. Go to Table Editor
2. Insert a new row in the `messages` table
3. Check your application logs for the realtime event

### 4. Test with SQL

```sql
INSERT INTO messages (room_id, user_name, content)
VALUES ('test-room', 'Test User', 'Hello from Supabase!');
```

## Troubleshooting

### Connection Issues

1. **Check your anon key**: Make sure it's correct
2. **Check network**: Ensure your server can reach Supabase
3. **Check logs**: Look for WebSocket errors

### Not Receiving Events

1. **Enable Realtime**: Make sure replication is enabled for your tables
2. **Check table names**: Ensure they match exactly
3. **Check RLS policies**: Row Level Security might block realtime events

### Enable Realtime Logs

Add to `application.yml`:

```yaml
logging:
  level:
    com.supabase.qnasession.service.SupabaseRealtimeService: DEBUG
```

## Advanced Configuration

### Subscribe to Specific Filters

Modify the subscription in `SupabaseRealtimeService`:

```java
Map<String, Object> filter = Map.of(
    "event", "INSERT",
    "schema", "public",
    "table", table,
    "filter", "room_id=eq." + roomId  // Only listen to specific room
);
message.put("payload", filter);
```

### Handle Connection Health

The service automatically:
- Sends heartbeat responses
- Reconnects on connection loss (5 second delay)
- Logs all connection states

## Security Notes

1. **Never commit your anon key**: Use environment variables in production
2. **Use RLS policies**: Secure your tables with Row Level Security
3. **Validate data**: Always validate incoming realtime data

## Production Checklist

- [ ] Replace hardcoded anon key with environment variable
- [ ] Enable proper RLS policies in Supabase
- [ ] Set up monitoring for WebSocket connection health
- [ ] Configure proper logging levels
- [ ] Test reconnection scenarios
- [ ] Add error handling for malformed events
- [ ] Consider rate limiting for high-volume tables

## Resources

- [Supabase Realtime Documentation](https://supabase.com/docs/guides/realtime)
- [Realtime Protocol](https://supabase.com/docs/guides/realtime/protocol)
- [Java WebSocket Client](https://github.com/TooTallNate/Java-WebSocket)
