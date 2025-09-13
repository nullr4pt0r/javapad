package live.javapad.v0.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * WebSocket observer implementation for broadcasting document changes to connected clients.
 * Replaces the manual broadcasting approach with the Observer pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationObserver implements DocumentObserver {

    private final SessionStore sessionStore;
    private final DocumentServiceV2 documentService;
    private final ObjectMapper objectMapper;

    @Override
    public void onDocumentUpdate(String docId, CollaborativeDocument document) {
        log.debug("Broadcasting document update to collaborators for document: {}", docId);
        broadcastToCollaborators(docId, document, "update");
    }

    @Override
    public void onDocumentCreated(String docId, CollaborativeDocument document) {
        log.debug("Broadcasting document creation to collaborators for document: {}", docId);
        broadcastToCollaborators(docId, document, "create");
    }

    @Override
    public void onDocumentDeleted(String docId) {
        log.debug("Broadcasting document deletion to collaborators for document: {}", docId);
        // For deletion, we broadcast to all sessions that had this document
        try {
            for (String sessionId : documentService.getCollaboratorsDetails(docId)) {
                WebSocketSession session = sessionStore.getSessionBySessionId(sessionId);
                if (session != null && session.isOpen()) {
                    Response response = Response.builder()
                            .docId(docId)
                            .sessionId(session.getId())
                            .event("delete")
                            .data("")
                            .docVersion(0)
                            .cursorPosition(0)
                            .build();
                    
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    session.sendMessage(new TextMessage(jsonResponse));
                    log.debug("Sent deletion notification to session: {}", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting document deletion for document {}: {}", docId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast document changes to all collaborators.
     *
     * @param docId    The document ID
     * @param document The document instance
     * @param event    The event type (update, create, etc.)
     */
    private void broadcastToCollaborators(String docId, CollaborativeDocument document, String event) {
        try {
            for (String sessionId : documentService.getCollaboratorsDetails(docId)) {
                WebSocketSession session = sessionStore.getSessionBySessionId(sessionId);
                if (session != null && session.isOpen()) {
                    Response response = Response.builder()
                            .docId(docId)
                            .sessionId(session.getId())
                            .event(event)
                            .data(document.getContent())
                            .docVersion(document.getVersion())
                            .cursorPosition(document.getContent().length() + 1)
                            .build();
                    
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    session.sendMessage(new TextMessage(jsonResponse));
                    log.debug("Sent {} notification to session: {} for document: {}", 
                             event, sessionId, docId);
                }
            }
            log.debug("Broadcast completed for document: {} with event: {}", docId, event);
        } catch (Exception e) {
            log.error("Error broadcasting document {} with event {}: {}", 
                     docId, event, e.getMessage(), e);
        }
    }
}