package live.javapad.v0.operationStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.dto.Response;
import live.javapad.v0.observer.DocumentEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;


@Service
@Slf4j
public class CreateOperationStrategy extends OperationStrategy{

    private final ObjectMapper objectMapper;
    private final SessionStore sessionStore;
    private final DocumentEventPublisher eventPublisher;

    protected CreateOperationStrategy(DocumentServiceV2 documentService, ObjectMapper objectMapper, 
                                     SessionStore sessionStore, DocumentEventPublisher eventPublisher) {
        super(documentService, sessionStore);
        this.objectMapper = objectMapper;
        this.sessionStore = sessionStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void apply(OperationRequest or, WebSocketSession webSocketSession) {
        CollaborativeDocument cd = documentService.createDocument(or);
        String content = or.getData();
        sessionStore.addDocumentToSession(webSocketSession.getId(), cd.getId());
        
        // Send immediate response to the creator
        try {
            String jsonResponse = objectMapper.writeValueAsString(Response.builder()
                    .docId(cd.getId())
                    .sessionId(webSocketSession.getId())
                    .event("create")
                    .data(content)
                    .cursorPosition(content.length() + 1)
                    .docVersion(cd.getVersion())
                    .build());
            webSocketSession.sendMessage(new TextMessage(jsonResponse));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize create response: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to send create response: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        
        // Use Observer pattern to notify about document creation
        eventPublisher.notifyDocumentCreated(cd.getId(), cd);
        log.debug("Document creation notification sent for document: {}", cd.getId());
    }
}
