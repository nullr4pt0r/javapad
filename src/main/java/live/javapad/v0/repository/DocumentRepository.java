package live.javapad.v0.repository;

import live.javapad.v0.CollaborativeDocument;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for document data access operations.
 * Implements the Repository pattern to abstract data storage implementation.
 */
public interface DocumentRepository {

    /**
     * Save a document to the repository.
     *
     * @param document The document to save
     * @return The saved document
     */
    CollaborativeDocument save(CollaborativeDocument document);

    /**
     * Find a document by its ID.
     *
     * @param documentId The document ID
     * @return Optional containing the document if found, empty otherwise
     */
    Optional<CollaborativeDocument> findById(String documentId);

    /**
     * Find all documents.
     *
     * @return List of all documents
     */
    List<CollaborativeDocument> findAll();

    /**
     * Find documents by collaborator session ID.
     *
     * @param sessionId The session ID
     * @return List of documents that have this session as a collaborator
     */
    List<CollaborativeDocument> findByCollaborator(String sessionId);

    /**
     * Delete a document by its ID.
     *
     * @param documentId The document ID
     * @return true if document was deleted, false if not found
     */
    boolean deleteById(String documentId);

    /**
     * Check if a document exists by its ID.
     *
     * @param documentId The document ID
     * @return true if document exists, false otherwise
     */
    boolean existsById(String documentId);

    /**
     * Get the total count of documents.
     *
     * @return Total number of documents
     */
    long count();

    /**
     * Update an existing document.
     *
     * @param document The document to update
     * @return The updated document
     * @throws IllegalArgumentException if document doesn't exist
     */
    CollaborativeDocument update(CollaborativeDocument document);

    /**
     * Find documents created after a specific version.
     *
     * @param minVersion The minimum version number
     * @return List of documents with version greater than minVersion
     */
    List<CollaborativeDocument> findByVersionGreaterThan(Integer minVersion);
}