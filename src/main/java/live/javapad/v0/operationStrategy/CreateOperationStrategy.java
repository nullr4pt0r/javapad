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


@Service
@Slf4j
public class CreateOperationStrategy extends OperationStrategy{


    private final ObjectMapper objectMapper;
    private final SessionStore sessionStore;

    protected CreateOperationStrategy(DocumentServiceV2 documentService, ObjectMapper objectMapper, SessionStore sessionStore) {
        super(documentService, sessionStore);
        this.objectMapper = objectMapper;
        this.sessionStore = sessionStore;
    }

    @Override
    public void apply(OperationRequest or, WebSocketSession webSocketSession) {
        CollaborativeDocument cd = documentService.createDocument(or);
        String content = or.getData();
        sessionStore.addDocumentToSession(webSocketSession.getId(), cd.getId());
        String jsonResponse = null;
        try {
            jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(cd.getId())
                    .sessionId(webSocketSession.getId()).event("create").data(content).cursorPosition(content.length()+1).
                    docVersion(cd.getVersion()).build());
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
