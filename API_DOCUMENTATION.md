# JavaPad API Documentation

## Overview
JavaPad is a collaborative text editor built with Spring Boot that provides real-time document editing capabilities through REST APIs and WebSocket connections. It supports multiple users editing the same document simultaneously with operational transformation to resolve conflicts.

## Architecture
- **Backend**: Spring Boot 3.5.3 with Java 17
- **Real-time Communication**: WebSockets
- **Document Storage**: In-memory (ConcurrentHashMap)
- **Design Patterns**: Strategy Pattern for operations, Factory Pattern for strategy selection

---

## REST API Endpoints

### Health Check
Check the application status and availability.

**Endpoint**: `GET /api/health`

**Description**: Returns a simple health check response to verify the application is running.

**Request**: No parameters required

**Response**: 
- **Status Code**: `200 OK`
- **Body**: Empty (Spring Boot default health check)

**Example**:
```bash
curl -X GET http://localhost:8080/api/health
```

**Response**:
```
HTTP/1.1 200 OK
```

---

### UI Access
Access the web-based text editor interface.

**Endpoint**: `GET /`

**Description**: Serves the main application UI (forwards to index.html).

**Request**: No parameters required

**Response**: 
- **Status Code**: `200 OK`
- **Content-Type**: `text/html`
- **Body**: HTML content for the text editor interface

---

**Endpoint**: `GET /open-ui`

**Description**: Redirects to the main application UI.

**Request**: No parameters required

**Response**: 
- **Status Code**: `302 Found`
- **Location**: `/`

---

## WebSocket API

### Connection Endpoint
Real-time collaborative editing functionality.

**Endpoint**: `ws://localhost:8080/editor`

**Description**: WebSocket endpoint for real-time document collaboration. Supports document creation, opening, and updates with operational transformation.

---

## Data Models

### OperationRequest
Request model for WebSocket operations.

```java
{
    "data": "string",              // Content data for the operation
    "cursorPosition": "integer",   // Current cursor position
    "endCursorPosition": "integer", // End cursor position (for delete operations)
    "docId": "string",             // Document identifier
    "docVersion": "integer",       // Document version for conflict resolution
    "sessionId": "string",         // Session identifier
    "event": "string",             // Operation type: "create", "open", "insert", "delete"
    "sessionName": "string"        // Human-readable session name
}
```

**Field Descriptions**:
- `data`: The text content being operated on
- `cursorPosition`: Starting position for insert/delete operations (0-based)
- `endCursorPosition`: Ending position for delete operations (inclusive)
- `docId`: UUID of the document being operated on
- `docVersion`: Version number for operational transformation conflict resolution
- `sessionId`: WebSocket session identifier
- `event`: Type of operation being performed
- `sessionName`: Optional human-readable name for the session

### Response
Response model for WebSocket operations.

```java
{
    "event": "string",           // Operation type that was performed
    "data": "string",            // Current document content
    "sessionId": "string",       // Session identifier
    "docId": "string",           // Document identifier
    "cursorPosition": "integer", // Suggested cursor position
    "docVersion": "integer"      // Current document version
}
```

**Field Descriptions**:
- `event`: Confirms the operation type that was processed
- `data`: Complete current content of the document
- `sessionId`: Session that performed the operation
- `docId`: UUID of the document
- `cursorPosition`: Recommended cursor position after the operation
- `docVersion`: Updated version number of the document

### CollaborativeDocument
Internal document model with versioning and collaboration features.

```java
{
    "id": "string",                    // Document UUID
    "content": "string",               // Current document content
    "version": "integer",              // Version number for conflict resolution
    "history": "OperationRequest[]",   // List of all operations performed
    "collaborators": "string[]"        // List of session IDs currently editing
}
```

---

## WebSocket Operations

### Create Document
Create a new collaborative document.

**Event Type**: `create`

**Request**:
```json
{
    "event": "create",
    "data": "Initial document content",
    "sessionId": "session-123",
    "sessionName": "User1"
}
```

**Response**:
```json
{
    "event": "create",
    "data": "Initial document content",
    "sessionId": "session-123",
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "cursorPosition": 25,
    "docVersion": 0
}
```

### Open Document
Open an existing document for editing.

**Event Type**: `open`

**Request**:
```json
{
    "event": "open",
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "sessionId": "session-456",
    "sessionName": "User2"
}
```

**Response**:
```json
{
    "event": "open",
    "data": "Current document content",
    "sessionId": "session-456",
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "cursorPosition": 24,
    "docVersion": 3
}
```

### Insert Text
Insert text at a specific position.

**Event Type**: `insert`

**Request**:
```json
{
    "event": "insert",
    "data": "Hello ",
    "cursorPosition": 10,
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "docVersion": 3,
    "sessionId": "session-123"
}
```

**Response**: Broadcast to all collaborators
```json
{
    "event": "update",
    "data": "Complete updated document content",
    "sessionId": "session-123",
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "cursorPosition": 30,
    "docVersion": 4
}
```

### Delete Text
Delete text between two positions.

**Event Type**: `delete`

**Request**:
```json
{
    "event": "delete",
    "cursorPosition": 5,
    "endCursorPosition": 10,
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "docVersion": 4,
    "sessionId": "session-123"
}
```

**Response**: Broadcast to all collaborators
```json
{
    "event": "update",
    "data": "Complete updated document content with deletion applied",
    "sessionId": "session-123",
    "docId": "550e8400-e29b-41d4-a716-446655440000",
    "cursorPosition": 5,
    "docVersion": 5
}
```

---

## Operational Transformation

The system implements operational transformation to handle concurrent edits:

### Insert-Insert Conflicts
When two users insert text at the same position:
- Operations are ordered by timestamp
- Later operations have their cursor positions adjusted based on earlier insertions

### Insert-Delete Conflicts
When one user inserts while another deletes:
- Delete operations adjust insert positions if the insert occurs within the deleted range
- Insert operations adjust delete ranges if text is inserted before the deletion

### Delete-Delete Conflicts
When two users delete overlapping ranges:
- Operations are merged to avoid double-deletion
- Cursor positions are recalculated to maintain document integrity

---

## Error Handling

### Common Error Responses
WebSocket error messages are sent as plain text:

```
"Error : Document Not Found!"
"Error : Key is invalid"
"Error : [Specific error message]"
```

### Error Scenarios
1. **Document Not Found**: Attempting to open a non-existent document
2. **Invalid Session**: Using an invalid or expired session ID
3. **Invalid Cursor Position**: Cursor position beyond document length
4. **JSON Parsing Error**: Malformed request JSON
5. **Network Errors**: WebSocket connection issues

---

## Connection Lifecycle

### Connection Establishment
1. Client connects to `ws://localhost:8080/editor`
2. Server assigns a unique session ID
3. Server sends confirmation: `"Session Connected:[session-id]"`

### Session Management
- Each WebSocket connection gets a unique session ID
- Sessions are mapped to active documents
- Session data is stored in-memory and cleaned up on disconnect

### Connection Termination
1. Client disconnects or connection is lost
2. Server removes session from active sessions
3. Server removes session from document collaborators
4. Other collaborators continue editing without interruption

---

## Performance Considerations

### Scalability Limitations
- **In-Memory Storage**: Documents are stored in ConcurrentHashMap (not persistent)
- **Single Instance**: No clustering support for horizontal scaling
- **Memory Usage**: All documents and history kept in memory

### Recommended Optimizations
1. Implement persistent storage (database)
2. Add connection pooling for WebSocket sessions
3. Implement document cleanup for inactive documents
4. Add rate limiting for operations
5. Use async message broadcasting
6. Implement document compression for large texts

---

## Security Considerations

### Current Implementation
- No authentication or authorization
- Open WebSocket connections
- No input validation or sanitization
- No rate limiting

### Recommended Security Enhancements
1. Add user authentication
2. Implement document access control
3. Add input validation and sanitization
4. Implement rate limiting per session
5. Add CORS configuration
6. Use secure WebSocket connections (WSS)