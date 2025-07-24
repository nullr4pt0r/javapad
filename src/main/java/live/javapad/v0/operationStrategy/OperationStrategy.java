package live.javapad.v0.operationStrategy;

import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import org.springframework.web.socket.WebSocketSession;


public abstract class OperationStrategy {
    final DocumentServiceV2 documentService;

    final SessionStore sessionStore;

    protected OperationStrategy(DocumentServiceV2 documentService, SessionStore sessionStore) {
        this.documentService = documentService;
        this.sessionStore = sessionStore;
    }

     public abstract void apply(OperationRequest operationRequest, WebSocketSession session);
}
