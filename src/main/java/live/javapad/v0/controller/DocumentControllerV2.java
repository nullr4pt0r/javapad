package live.javapad.v0.controller;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.command.*;
import live.javapad.v0.memento.DocumentMemento;
import live.javapad.v0.memento.DocumentMementoManager;
import live.javapad.v0.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced controller demonstrating Repository pattern, Command pattern, and Memento pattern usage.
 * Provides REST API endpoints for advanced document operations.
 */
@RestController
@RequestMapping("/api/v2/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentControllerV2 {

    private final DocumentRepository documentRepository;
    private final CommandProcessor commandProcessor;
    private final DocumentMementoManager mementoManager;

    /**
     * Get all documents using Repository pattern.
     */
    @GetMapping
    public ResponseEntity<List<CollaborativeDocument>> getAllDocuments() {
        List<CollaborativeDocument> documents = documentRepository.findAll();
        log.debug("Retrieved {} documents", documents.size());
        return ResponseEntity.ok(documents);
    }

    /**
     * Get a specific document by ID using Repository pattern.
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<CollaborativeDocument> getDocument(@PathVariable String documentId) {
        Optional<CollaborativeDocument> document = documentRepository.findById(documentId);
        if (document.isPresent()) {
            log.debug("Retrieved document: {}", documentId);
            return ResponseEntity.ok(document.get());
        } else {
            log.debug("Document not found: {}", documentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get documents for a specific collaborator using Repository pattern.
     */
    @GetMapping("/collaborator/{sessionId}")
    public ResponseEntity<List<CollaborativeDocument>> getDocumentsByCollaborator(@PathVariable String sessionId) {
        List<CollaborativeDocument> documents = documentRepository.findByCollaborator(sessionId);
        log.debug("Retrieved {} documents for collaborator: {}", documents.size(), sessionId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Undo last operation using Command pattern.
     */
    @PostMapping("/{documentId}/undo")
    public ResponseEntity<Map<String, Object>> undoLastOperation(@PathVariable String documentId) {
        boolean success = commandProcessor.undoLastCommand(documentId);
        Map<String, Object> response = Map.of(
            "success", success,
            "undoCount", commandProcessor.getUndoCount(documentId),
            "redoCount", commandProcessor.getRedoCount(documentId)
        );
        log.debug("Undo operation for document {}: {}", documentId, success ? "success" : "failed");
        return ResponseEntity.ok(response);
    }

    /**
     * Redo last undone operation using Command pattern.
     */
    @PostMapping("/{documentId}/redo")
    public ResponseEntity<Map<String, Object>> redoLastOperation(@PathVariable String documentId) {
        boolean success = commandProcessor.redoLastCommand(documentId);
        Map<String, Object> response = Map.of(
            "success", success,
            "undoCount", commandProcessor.getUndoCount(documentId),
            "redoCount", commandProcessor.getRedoCount(documentId)
        );
        log.debug("Redo operation for document {}: {}", documentId, success ? "success" : "failed");
        return ResponseEntity.ok(response);
    }

    /**
     * Create a snapshot using Memento pattern.
     */
    @PostMapping("/{documentId}/snapshot")
    public ResponseEntity<Map<String, Object>> createSnapshot(@PathVariable String documentId) {
        Optional<CollaborativeDocument> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentMemento memento = mementoManager.createSnapshot(documentOpt.get());
        Map<String, Object> response = Map.of(
            "success", true,
            "snapshot", Map.of(
                "version", memento.getVersion(),
                "timestamp", memento.getTimestamp(),
                "description", memento.getDescription()
            ),
            "snapshotCount", mementoManager.getSnapshotCount(documentId)
        );
        log.debug("Created snapshot for document: {}", documentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all snapshots for a document using Memento pattern.
     */
    @GetMapping("/{documentId}/snapshots")
    public ResponseEntity<List<Map<String, Object>>> getSnapshots(@PathVariable String documentId) {
        List<DocumentMemento> snapshots = mementoManager.getAllSnapshots(documentId);
        List<Map<String, Object>> response = snapshots.stream()
            .map(memento -> Map.<String, Object>of(
                "version", memento.getVersion(),
                "timestamp", memento.getTimestamp(),
                "description", memento.getDescription(),
                "contentLength", memento.getContent().length(),
                "collaboratorCount", memento.getCollaborators().size()
            ))
            .toList();
        
        log.debug("Retrieved {} snapshots for document: {}", response.size(), documentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Restore document from a specific snapshot using Memento pattern.
     */
    @PostMapping("/{documentId}/restore/{version}")
    public ResponseEntity<Map<String, Object>> restoreFromSnapshot(
            @PathVariable String documentId, 
            @PathVariable Integer version) {
        
        Optional<CollaborativeDocument> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<DocumentMemento> mementoOpt = mementoManager.getSnapshotByVersion(documentId, version);
        if (mementoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Snapshot with version " + version + " not found"
            ));
        }

        CollaborativeDocument document = documentOpt.get();
        mementoManager.restoreFromSnapshot(document, mementoOpt.get());
        documentRepository.update(document);

        Map<String, Object> response = Map.of(
            "success", true,
            "restoredVersion", version,
            "currentVersion", document.getVersion(),
            "contentLength", document.getContent().length()
        );
        
        log.debug("Restored document {} from snapshot version {}", documentId, version);
        return ResponseEntity.ok(response);
    }

    /**
     * Get repository statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = Map.of(
            "totalDocuments", documentRepository.count(),
            "repositoryType", documentRepository.getClass().getSimpleName()
        );
        return ResponseEntity.ok(stats);
    }
}