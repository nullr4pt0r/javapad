package live.javapad.v0.operationStrategy;

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
public class UpdateOperationStrategy extends OperationStrategy{

    private final ObjectMapper objectMapper;

    protected UpdateOperationStrategy(DocumentServiceV2 documentService, ObjectMapper objectMapper, SessionStore sessionStore) {
        super(documentService, sessionStore);
        this.objectMapper = objectMapper;
    }

    public void apply(OperationRequest or, WebSocketSession session) {
        documentService.updateDocument(or);
        try {
            broadcastDocumentToAll(or.getDocId());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void broadcastDocumentToAll(String documentId) throws IOException {
        CollaborativeDocument cd = documentService.getDocument(documentId);

        for(String sessionId: documentService.getCollaboratorsDetails(documentId)){
            WebSocketSession session = sessionStore.getSessionBySessionId(sessionId);
            Response response = Response.builder().docId(documentId).sessionId(session.getId()).event("update").data(cd.getContent())
                    .docVersion(cd.getVersion())
                    .cursorPosition(cd.getContent().length()+1).build();
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
            log.info("Send : {} to session : {}", cd.getContent(), sessionId);
        }

        log.info("Broadcast completed !");
    }

}
