package live.javapad.v0.repository;

import live.javapad.v0.CollaborativeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DocumentRepository.
 * Replaces direct ConcurrentHashMap usage with repository pattern.
 */
@Repository
@Slf4j
public class InMemoryDocumentRepository implements DocumentRepository {

    private final ConcurrentHashMap<String, CollaborativeDocument> documents = new ConcurrentHashMap<>();

    @Override
    public CollaborativeDocument save(CollaborativeDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        
        if (document.getId() == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        
        documents.put(document.getId(), document);
        log.debug("Saved document with ID: {}", document.getId());
        return document;
    }

    @Override
    public Optional<CollaborativeDocument> findById(String documentId) {
        if (documentId == null) {
            return Optional.empty();
        }
        
        CollaborativeDocument document = documents.get(documentId);
        log.debug("Finding document by ID {}: {}", documentId, document != null ? "found" : "not found");
        return Optional.ofNullable(document);
    }

    @Override
    public List<CollaborativeDocument> findAll() {
        List<CollaborativeDocument> allDocuments = List.copyOf(documents.values());
        log.debug("Retrieved {} documents", allDocuments.size());
        return allDocuments;
    }

    @Override
    public List<CollaborativeDocument> findByCollaborator(String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        
        List<CollaborativeDocument> collaboratorDocuments = documents.values().stream()
                .filter(doc -> doc.getCollaborators() != null && 
                              doc.getCollaborators().contains(sessionId))
                .collect(Collectors.toList());
                
        log.debug("Found {} documents for collaborator: {}", collaboratorDocuments.size(), sessionId);
        return collaboratorDocuments;
    }

    @Override
    public boolean deleteById(String documentId) {
        if (documentId == null) {
            return false;
        }
        
        CollaborativeDocument removed = documents.remove(documentId);
        boolean deleted = removed != null;
        log.debug("Delete document {}: {}", documentId, deleted ? "success" : "not found");
        return deleted;
    }

    @Override
    public boolean existsById(String documentId) {
        if (documentId == null) {
            return false;
        }
        
        boolean exists = documents.containsKey(documentId);
        log.debug("Document {} exists: {}", documentId, exists);
        return exists;
    }

    @Override
    public long count() {
        long count = documents.size();
        log.debug("Total document count: {}", count);
        return count;
    }

    @Override
    public CollaborativeDocument update(CollaborativeDocument document) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Document and document ID cannot be null");
        }
        
        if (!documents.containsKey(document.getId())) {
            throw new IllegalArgumentException("Document with ID " + document.getId() + " does not exist");
        }
        
        documents.put(document.getId(), document);
        log.debug("Updated document with ID: {}", document.getId());
        return document;
    }

    @Override
    public List<CollaborativeDocument> findByVersionGreaterThan(Integer minVersion) {
        if (minVersion == null) {
            return findAll();
        }
        
        List<CollaborativeDocument> filteredDocuments = documents.values().stream()
                .filter(doc -> doc.getVersion() != null && doc.getVersion() > minVersion)
                .collect(Collectors.toList());
                
        log.debug("Found {} documents with version > {}", filteredDocuments.size(), minVersion);
        return filteredDocuments;
    }

    /**
     * Clear all documents (for testing purposes).
     */
    public void clear() {
        int size = documents.size();
        documents.clear();
        log.debug("Cleared {} documents from repository", size);
    }

    /**
     * Get internal map size for testing/monitoring.
     *
     * @return Current number of documents in memory
     */
    public int size() {
        return documents.size();
    }
}