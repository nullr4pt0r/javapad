package live.javapad.v0.memento;

import live.javapad.v0.dto.OperationRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Memento class for storing document state snapshots.
 * Implements the Memento pattern for robust state management.
 */
@Data
@RequiredArgsConstructor
public class DocumentMemento {
    
    private final String documentId;
    private final String content;
    private final Integer version;
    private final List<String> collaborators;
    private final List<OperationRequest> history;
    private final LocalDateTime timestamp;

    /**
     * Create a memento from current state.
     *
     * @param documentId   The document ID
     * @param content      The document content
     * @param version      The document version
     * @param collaborators List of collaborator session IDs
     * @param history      Operation history
     * @return New memento instance
     */
    public static DocumentMemento create(String documentId, String content, Integer version,
                                       List<String> collaborators, List<OperationRequest> history) {
        return new DocumentMemento(
            documentId,
            content,
            version,
            List.copyOf(collaborators != null ? collaborators : List.of()),
            List.copyOf(history != null ? history : List.of()),
            LocalDateTime.now()
        );
    }

    /**
     * Get a description of this memento for logging.
     *
     * @return Memento description
     */
    public String getDescription() {
        return String.format("Document %s v%d at %s (content length: %d, collaborators: %d)", 
                           documentId, version, timestamp, 
                           content != null ? content.length() : 0,
                           collaborators.size());
    }
}