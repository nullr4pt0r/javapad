# Design Patterns Implementation in JavaPad

This document shows the actual code pieces from the JavaPad repository that implement design patterns, mapping them to the optimization recommendations provided.

## Currently Implemented Design Patterns

### 1. Strategy Pattern (✅ Already Implemented)

The Strategy pattern is well-implemented in the `operationStrategy` package for handling different document operations.

**Base Strategy Interface:**
```java
// src/main/java/live/javapad/v0/operationStrategy/OperationStrategy.java
public abstract class OperationStrategy {
    final DocumentServiceV2 documentService;
    final SessionStore sessionStore;

    protected OperationStrategy(DocumentServiceV2 documentService, SessionStore sessionStore) {
        this.documentService = documentService;
        this.sessionStore = sessionStore;
    }

    public abstract void apply(OperationRequest operationRequest, WebSocketSession session);
}
```

**Concrete Strategy Implementations:**

1. **Create Operation Strategy:**
```java
// src/main/java/live/javapad/v0/operationStrategy/CreateOperationStrategy.java
@Service
@Slf4j
public class CreateOperationStrategy extends OperationStrategy {
    private final ObjectMapper objectMapper;
    private final SessionStore sessionStore;

    @Override
    public void apply(OperationRequest or, WebSocketSession webSocketSession) {
        CollaborativeDocument cd = documentService.createDocument(or);
        String content = or.getData();
        sessionStore.addDocumentToSession(webSocketSession.getId(), cd.getId());
        
        // Build and send response
        String jsonResponse = objectMapper.writeValueAsString(
            Response.builder()
                .docId(cd.getId())
                .sessionId(webSocketSession.getId())
                .event("create")
                .data(content)
                .cursorPosition(content.length()+1)
                .docVersion(cd.getVersion())
                .build()
        );
        webSocketSession.sendMessage(new TextMessage(jsonResponse));
    }
}
```

2. **Update Operation Strategy:**
```java
// src/main/java/live/javapad/v0/operationStrategy/UpdateOperationStrategy.java
@Slf4j
@Service
public class UpdateOperationStrategy extends OperationStrategy {
    private final ObjectMapper objectMapper;

    public void apply(OperationRequest or, WebSocketSession session) {
        documentService.updateDocument(or);
        broadcastDocumentToAll(or.getDocId());
    }

    public void broadcastDocumentToAll(String documentId) throws IOException {
        CollaborativeDocument cd = documentService.getDocument(documentId);
        
        for(String sessionId: documentService.getCollaboratorsDetails(documentId)){
            WebSocketSession session = sessionStore.getSessionBySessionId(sessionId);
            Response response = Response.builder()
                .docId(documentId)
                .sessionId(session.getId())
                .event("update")
                .data(cd.getContent())
                .docVersion(cd.getVersion())
                .cursorPosition(cd.getContent().length()+1)
                .build();
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
        }
    }
}
```

### 2. Factory Pattern (✅ Already Implemented)

The Factory pattern is implemented to select the appropriate operation strategy based on the operation type.

```java
// src/main/java/live/javapad/v0/operationStrategy/OperationStrategyFactory.java
@AllArgsConstructor
@Component
public class OperationStrategyFactory {
    private final CreateOperationStrategy createOperationStrategy;
    private final OpenOperationStrategy openOperationStrategy;
    private final UpdateOperationStrategy updateOperationStrategy;

    public OperationStrategy getOperationStrategy(String operation){
        if (operation == null) {
            return null;
        }
        switch (operation){
            case "create" :
                return createOperationStrategy;
            case "open":
                return openOperationStrategy;
            case "insert":
            case "delete":
                return updateOperationStrategy;
        }
        return null;
    }
}
```

### 3. Builder Pattern (✅ Already Implemented with Lombok)

The Builder pattern is implemented using Lombok annotations in the main domain model.

```java
// src/main/java/live/javapad/v0/CollaborativeDocument.java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollaborativeDocument {
    private String id;
    private String content;
    @Builder.Default
    private Integer version = 0;
    @Builder.Default
    private List<OperationRequest> history = new ArrayList<>();
    @Builder.Default
    private List<String> collaborators = new ArrayList<>();
}
```

**Usage in Document Service:**
```java
// src/main/java/live/javapad/v0/document/service/DocumentServiceV2.java
public CollaborativeDocument createDocument(OperationRequest operationRequest){
    String docId = String.valueOf(UUID.randomUUID());
    CollaborativeDocument cd = createCollaborativeDocument(operationRequest, docId);
    documentStore.addDocument(docId, cd);
    return cd;
}

private CollaborativeDocument createCollaborativeDocument(OperationRequest operationRequest, String docId){
    CollaborativeDocument collaborativeDocument = new CollaborativeDocument();
    collaborativeDocument.setId(docId);
    collaborativeDocument.setContent(operationRequest.getData());
    collaborativeDocument.getCollaborators().add(operationRequest.getSessionId());
    collaborativeDocument.getHistory().add(operationRequest);
    return collaborativeDocument;
}
```

### 4. In-Memory Store Pattern (✅ Currently Implemented)

The current data storage uses a simple in-memory approach with ConcurrentHashMap.

```java
// src/main/java/live/javapad/v0/datastore/DocumentStore.java
@Component
public final class DocumentStore {
    private static final ConcurrentHashMap<String, CollaborativeDocument> documents = new ConcurrentHashMap<>();

    public void addDocument(String key, CollaborativeDocument doc){
        documents.put(key, doc);
    }

    public CollaborativeDocument getDocumentByKey(String key){
        if(key != null) {
            verifyDocumentExistence(key);
            return documents.get(key);
        }
        throw new RuntimeException("Key is invalid");
    }

    public void verifyDocumentExistence(String documentId) {
        if(!documents.containsKey(documentId)){
            throw new RuntimeException("Document Not Found!");
        }
    }

    public void removeDocument(String key){
        verifyDocumentExistence(key);
        documents.remove(key);
    }
}
```

## Design Patterns Recommended for Implementation

### 1. Observer Pattern (🔄 Recommended Enhancement)

**Current Implementation:** Manual broadcasting in `UpdateOperationStrategy.broadcastDocumentToAll()`

**Current Code:**
```java
// Manual broadcasting approach (from UpdateOperationStrategy.java)
public void broadcastDocumentToAll(String documentId) throws IOException {
    CollaborativeDocument cd = documentService.getDocument(documentId);
    
    for(String sessionId: documentService.getCollaboratorsDetails(documentId)){
        WebSocketSession session = sessionStore.getSessionBySessionId(sessionId);
        // ... manual message sending to each session
    }
}
```

**Recommended Enhancement:** Implement Observer pattern as shown in the optimization recommendations to decouple notification logic.

### 2. Command Pattern (🔄 Recommended for Undo/Redo)

**Current Implementation:** Direct operation processing without queuing

**Current Code:**
```java
// Direct processing in DocumentServiceV2.java
public void updateDocument(OperationRequest or){
    documentStore.verifyDocumentExistence(or.getDocId());
    handleTransformation(or);
}

void handleTransformation(OperationRequest request){
    CollaborativeDocument document = documentStore.getDocumentByKey(request.getDocId());
    Integer currentDocumentVersion = document.getVersion();
    if(currentDocumentVersion > request.getDocVersion()){
        OperationRequest previous = document.getHistory().get(document.getHistory().size()-1);
        transformDocumentOnVersionDifference(previous, request);
    }else{
        transformDocumentContent(request);
    }
}
```

**Recommended Enhancement:** Implement Command pattern as shown in optimization recommendations for undo/redo functionality.

### 3. Repository Pattern (🔄 Recommended Enhancement)

**Current Implementation:** Direct ConcurrentHashMap usage

**Current Code:**
```java
// Direct data access in DocumentStore.java
private static final ConcurrentHashMap<String, CollaborativeDocument> documents = new ConcurrentHashMap<>();
```

**Recommended Enhancement:** Implement Repository pattern interface as shown in optimization recommendations for better testability and flexibility.

### 4. Operational Transformation Pattern (✅ Partially Implemented)

The application implements basic operational transformation for handling concurrent edits.

```java
// src/main/java/live/javapad/v0/document/service/DocumentServiceV2.java
CollaborativeDocument transformDocumentOnVersionDifference(OperationRequest previous, OperationRequest current){
    String previousOperation = previous.getEvent();
    String currentOperation = current.getEvent();
    CollaborativeDocument doc = documentStore.getDocumentByKey(current.getDocId());
    
    if(previousOperation.equals("insert") && currentOperation.equals("insert")){
        if(current.getCursorPosition() >= previous.getCursorPosition()){
            current.setCursorPosition(current.getCursorPosition()+previous.getData().length());
        }
    }else if(previousOperation.equals("insert") && currentOperation.equals("delete")){
        if(previous.getCursorPosition() < current.getCursorPosition()){
            int length = previous.getData().length();
            current.setCursorPosition(current.getCursorPosition()+length);
            current.setEndCursorPosition(current.getEndCursorPosition()+length);
        }
    }
    // ... more transformation logic
    
    transformDocumentContent(current);
    return doc;
}

private void transformDocumentContent(OperationRequest operationRequest){
    CollaborativeDocument doc = documentStore.getDocumentByKey(operationRequest.getDocId());
    StringBuilder docContent = new StringBuilder(doc.getContent());
    
    if(operationRequest.getEvent().equals("insert")){
        int pos = operationRequest.getCursorPosition();
        if(pos > docContent.length()){
            pos = docContent.length();
        }
        docContent.insert(pos,operationRequest.getData());
    }else if(operationRequest.getEvent().equals("delete")){
        if(operationRequest.getCursorPosition() <= operationRequest.getEndCursorPosition()){
            docContent.delete(operationRequest.getCursorPosition(), operationRequest.getEndCursorPosition()+1);
        }
    }
    doc.setContent(docContent.toString());
    doc.setVersion(doc.getVersion()+1);
    doc.getHistory().add(operationRequest);
    documentStore.addDocument(operationRequest.getDocId(), doc);
}
```

## Summary

The JavaPad codebase already implements several solid design patterns:

### ✅ **Well Implemented:**
1. **Strategy Pattern** - Clean separation of operation handling logic
2. **Factory Pattern** - Centralized strategy selection
3. **Builder Pattern** - Using Lombok for clean object construction
4. **Operational Transformation** - Basic conflict resolution for concurrent edits

### 🔄 **Areas for Enhancement:**
1. **Observer Pattern** - To replace manual broadcasting with event-driven notifications
2. **Command Pattern** - To enable undo/redo functionality and operation queuing
3. **Repository Pattern** - To abstract data access and improve testability
4. **Memento Pattern** - To provide robust state management for undo/redo

The current architecture provides a solid foundation that can be enhanced with the recommended patterns to improve scalability, maintainability, and user experience.