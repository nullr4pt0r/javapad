package live.javapad.v0.operationStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;


@Slf4j
@Service
public class OpenOperationStrategy extends OperationStrategy{

    private final ObjectMapper objectMapper;
    protected OpenOperationStrategy(DocumentServiceV2 documentService, ObjectMapper objectMapper, SessionStore sessionStore) {
        super(documentService, sessionStore);
        this.objectMapper = objectMapper;
    }

    @Override
    public void apply(OperationRequest or, WebSocketSession webSocketSession){
        String documentId = or.getDocId();
        CollaborativeDocument doc = documentService.fetchAndAddCollaborator(documentId, or.getSessionId());
        sessionStore.addDocumentToSession(webSocketSession.getId(), documentId);
         String jsonResponse = null;
         try {
             jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(documentId)
                     .sessionId(webSocketSession.getId()).event("open").data(doc.getContent()).cursorPosition(doc.getContent().length()+1)
                     .docVersion(doc.getVersion()).build());
         } catch (JsonProcessingException e) {
             log.error(e.getMessage());
             throw new RuntimeException(e);
         }
         try {
             webSocketSession.sendMessage(new TextMessage(jsonResponse));
         } catch (IOException e) {
             log.error(e.getMessage());
             throw new RuntimeException(e);
         }
     }

}
