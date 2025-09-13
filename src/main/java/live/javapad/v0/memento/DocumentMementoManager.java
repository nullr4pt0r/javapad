package live.javapad.v0.memento;

import live.javapad.v0.CollaborativeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.List;
import java.util.Optional;

/**
 * Caretaker class for managing document mementos.
 * Handles snapshot creation, storage, and restoration.
 */
@Component
@Slf4j
public class DocumentMementoManager {
    
    private final ConcurrentHashMap<String, Deque<DocumentMemento>> documentSnapshots = new ConcurrentHashMap<>();
    private static final int MAX_SNAPSHOTS_PER_DOCUMENT = 20;

    /**
     * Create and store a snapshot of a document's current state.
     *
     * @param document The document to snapshot
     * @return The created memento
     */
    public DocumentMemento createSnapshot(CollaborativeDocument document) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Document and document ID cannot be null");
        }

        DocumentMemento memento = DocumentMemento.create(
            document.getId(),
            document.getContent(),
            document.getVersion(),
            document.getCollaborators(),
            document.getHistory()
        );

        // Store the snapshot
        Deque<DocumentMemento> snapshots = documentSnapshots.computeIfAbsent(
            document.getId(), k -> new ConcurrentLinkedDeque<>()
        );
        
        snapshots.addLast(memento);
        
        // Limit the number of snapshots
        if (snapshots.size() > MAX_SNAPSHOTS_PER_DOCUMENT) {
            DocumentMemento removed = snapshots.removeFirst();
            log.debug("Removed old snapshot: {}", removed.getDescription());
        }

        log.debug("Created snapshot: {}", memento.getDescription());
        return memento;
    }

    /**
     * Restore a document to a previous state using a memento.
     *
     * @param document The document to restore
     * @param memento  The memento containing the state to restore
     */
    public void restoreFromSnapshot(CollaborativeDocument document, DocumentMemento memento) {
        if (document == null || memento == null) {
            throw new IllegalArgumentException("Document and memento cannot be null");
        }

        if (!document.getId().equals(memento.getDocumentId())) {
            throw new IllegalArgumentException("Document ID mismatch");
        }

        document.setContent(memento.getContent());
        document.setVersion(memento.getVersion());
        document.getCollaborators().clear();
        document.getCollaborators().addAll(memento.getCollaborators());
        document.getHistory().clear();
        document.getHistory().addAll(memento.getHistory());

        log.debug("Restored document from snapshot: {}", memento.getDescription());
    }

    /**
     * Get the most recent snapshot for a document.
     *
     * @param documentId The document ID
     * @return Optional containing the most recent memento, empty if none exists
     */
    public Optional<DocumentMemento> getLatestSnapshot(String documentId) {
        if (documentId == null) {
            return Optional.empty();
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.get(documentId);
        if (snapshots == null || snapshots.isEmpty()) {
            return Optional.empty();
        }

        DocumentMemento latest = snapshots.peekLast();
        log.debug("Retrieved latest snapshot for document {}: {}", documentId, 
                 latest != null ? latest.getDescription() : "none");
        return Optional.ofNullable(latest);
    }

    /**
     * Get all snapshots for a document.
     *
     * @param documentId The document ID
     * @return List of mementos for the document
     */
    public List<DocumentMemento> getAllSnapshots(String documentId) {
        if (documentId == null) {
            return List.of();
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.get(documentId);
        if (snapshots == null) {
            return List.of();
        }

        List<DocumentMemento> snapshotList = List.copyOf(snapshots);
        log.debug("Retrieved {} snapshots for document {}", snapshotList.size(), documentId);
        return snapshotList;
    }

    /**
     * Get a snapshot by version for a document.
     *
     * @param documentId The document ID
     * @param version    The version to find
     * @return Optional containing the memento with matching version, empty if not found
     */
    public Optional<DocumentMemento> getSnapshotByVersion(String documentId, Integer version) {
        if (documentId == null || version == null) {
            return Optional.empty();
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.get(documentId);
        if (snapshots == null) {
            return Optional.empty();
        }

        Optional<DocumentMemento> found = snapshots.stream()
                .filter(memento -> version.equals(memento.getVersion()))
                .findFirst();

        log.debug("Searched for snapshot with version {} in document {}: {}", 
                 version, documentId, found.isPresent() ? "found" : "not found");
        return found;
    }

    /**
     * Remove all snapshots for a document.
     *
     * @param documentId The document ID
     * @return Number of snapshots removed
     */
    public int clearSnapshots(String documentId) {
        if (documentId == null) {
            return 0;
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.remove(documentId);
        int removed = snapshots != null ? snapshots.size() : 0;
        log.debug("Cleared {} snapshots for document {}", removed, documentId);
        return removed;
    }

    /**
     * Get the number of snapshots for a document.
     *
     * @param documentId The document ID
     * @return Number of snapshots
     */
    public int getSnapshotCount(String documentId) {
        if (documentId == null) {
            return 0;
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.get(documentId);
        return snapshots != null ? snapshots.size() : 0;
    }

    /**
     * Remove snapshots older than a specific timestamp.
     *
     * @param documentId The document ID
     * @param before     Only keep snapshots after this timestamp
     * @return Number of snapshots removed
     */
    public int removeSnapshotsOlderThan(String documentId, LocalDateTime before) {
        if (documentId == null || before == null) {
            return 0;
        }

        Deque<DocumentMemento> snapshots = documentSnapshots.get(documentId);
        if (snapshots == null) {
            return 0;
        }

        int originalSize = snapshots.size();
        snapshots.removeIf(memento -> memento.getTimestamp().isBefore(before));
        int removed = originalSize - snapshots.size();

        log.debug("Removed {} old snapshots for document {} (before {})", 
                 removed, documentId, before);
        return removed;
    }
}