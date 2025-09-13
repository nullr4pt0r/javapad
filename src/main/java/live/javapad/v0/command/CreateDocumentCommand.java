package live.javapad.v0.command;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.observer.DocumentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command for creating a new document.
 * Implements the Command pattern with undo capability.
 */
@RequiredArgsConstructor
@Slf4j
public class CreateDocumentCommand implements Command {
    
    private final String documentId;
    private final String sessionId;
    private final String initialContent;
    private final DocumentServiceV2 documentService;
    private final DocumentEventPublisher eventPublisher;

    @Override
    public void execute() {
        try {
            log.debug("Executing create document command for document {}", documentId);
            
            CollaborativeDocument document = documentService.createDocumentWithId(
                documentId, initialContent, sessionId);
            
            // Publish event through observer pattern
            eventPublisher.notifyDocumentCreated(documentId, document);
                
        } catch (Exception e) {
            log.error("Failed to execute create document command: {}", e.getMessage(), e);
            throw new RuntimeException("Create document command execution failed", e);
        }
    }

    @Override
    public void undo() {
        try {
            log.debug("Undoing create document command for document {}", documentId);
            documentService.deleteDocument(documentId);
            
            // Publish event through observer pattern
            eventPublisher.notifyDocumentDeleted(documentId);
                
        } catch (Exception e) {
            log.error("Failed to undo create document command: {}", e.getMessage(), e);
            throw new RuntimeException("Create document command undo failed", e);
        }
    }

    @Override
    public String getDocumentId() {
        return documentId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getDescription() {
        return String.format("Create document '%s' with initial content", documentId);
    }
}