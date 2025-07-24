package live.javapad.v0.session.service;

import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@AllArgsConstructor
@Slf4j
public class SessionService {

    private final SessionStore sessionStore;
    private final DocumentServiceV2 documentService;
    public void removeDocuments(WebSocketSession session){
        String sessionId = session.getId();
        if(sessionStore.verifySessionExistence(sessionId)){
            String documentId = sessionStore.getDocumentIdBySessionId(sessionId);
            sessionStore.removeDocumentId(sessionId);
            documentService.removeDocument(documentId);
            log.info("Document removed for session : {}", sessionId);
            sessionStore.removeSession(sessionId);
        }

    }
}
