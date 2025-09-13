package live.javapad.v0.operationStrategy;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.observer.DocumentEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class UpdateOperationStrategy extends OperationStrategy{

    private final DocumentEventPublisher eventPublisher;

    protected UpdateOperationStrategy(DocumentServiceV2 documentService, SessionStore sessionStore, 
                                     DocumentEventPublisher eventPublisher) {
        super(documentService, sessionStore);
        this.eventPublisher = eventPublisher;
    }

    public void apply(OperationRequest or, WebSocketSession session) {
        documentService.updateDocument(or);
        
        // Use Observer pattern instead of manual broadcasting
        CollaborativeDocument updatedDocument = documentService.getDocument(or.getDocId());
        eventPublisher.notifyDocumentUpdate(or.getDocId(), updatedDocument);
        
        log.debug("Document update notification sent for document: {}", or.getDocId());
    }
}
