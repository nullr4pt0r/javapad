package live.javapad.v0.command;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.observer.DocumentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command for deleting text from a document.
 * Implements the Command pattern with undo capability.
 */
@RequiredArgsConstructor
@Slf4j
public class DeleteTextCommand implements Command {
    
    private final String documentId;
    private final String sessionId;
    private final int startPosition;
    private final int endPosition;
    private final DocumentServiceV2 documentService;
    private final DocumentEventPublisher eventPublisher;
    
    // Store deleted text for undo operation
    private String deletedText;

    @Override
    public void execute() {
        try {
            log.debug("Executing delete command: positions {}-{} in document {}", 
                     startPosition, endPosition, documentId);
            
            // Store the text that will be deleted for undo
            CollaborativeDocument document = documentService.getDocument(documentId);
            String content = document.getContent();
            
            if (startPosition >= 0 && endPosition <= content.length() && startPosition <= endPosition) {
                deletedText = content.substring(startPosition, endPosition);
                documentService.deleteText(documentId, startPosition, endPosition);
                
                // Publish event through observer pattern
                eventPublisher.notifyDocumentUpdate(documentId, 
                    documentService.getDocument(documentId));
            } else {
                throw new IllegalArgumentException("Invalid delete positions");
            }
                
        } catch (Exception e) {
            log.error("Failed to execute delete command: {}", e.getMessage(), e);
            throw new RuntimeException("Delete command execution failed", e);
        }
    }

    @Override
    public void undo() {
        try {
            log.debug("Undoing delete command: inserting '{}' at position {} in document {}", 
                     deletedText, startPosition, documentId);
            
            if (deletedText != null) {
                documentService.insertText(documentId, deletedText, startPosition);
                
                // Publish event through observer pattern
                eventPublisher.notifyDocumentUpdate(documentId, 
                    documentService.getDocument(documentId));
            } else {
                log.warn("Cannot undo delete command: deleted text not stored");
            }
                
        } catch (Exception e) {
            log.error("Failed to undo delete command: {}", e.getMessage(), e);
            throw new RuntimeException("Delete command undo failed", e);
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
        return String.format("Delete text from position %d to %d", startPosition, endPosition);
    }
}