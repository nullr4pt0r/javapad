package live.javapad.v0.command;

import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.observer.DocumentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command for inserting text into a document.
 * Implements the Command pattern with undo capability.
 */
@RequiredArgsConstructor
@Slf4j
public class InsertTextCommand implements Command {
    
    private final String documentId;
    private final String sessionId;
    private final String text;
    private final int position;
    private final DocumentServiceV2 documentService;
    private final DocumentEventPublisher eventPublisher;

    @Override
    public void execute() {
        try {
            log.debug("Executing insert command: '{}' at position {} in document {}", 
                     text, position, documentId);
            documentService.insertText(documentId, text, position);
            
            // Publish event through observer pattern
            eventPublisher.notifyDocumentUpdate(documentId, 
                documentService.getDocument(documentId));
                
        } catch (Exception e) {
            log.error("Failed to execute insert command: {}", e.getMessage(), e);
            throw new RuntimeException("Insert command execution failed", e);
        }
    }

    @Override
    public void undo() {
        try {
            log.debug("Undoing insert command: removing '{}' from position {} in document {}", 
                     text, position, documentId);
            documentService.deleteText(documentId, position, position + text.length());
            
            // Publish event through observer pattern
            eventPublisher.notifyDocumentUpdate(documentId, 
                documentService.getDocument(documentId));
                
        } catch (Exception e) {
            log.error("Failed to undo insert command: {}", e.getMessage(), e);
            throw new RuntimeException("Insert command undo failed", e);
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
        return String.format("Insert '%s' at position %d", text, position);
    }
}