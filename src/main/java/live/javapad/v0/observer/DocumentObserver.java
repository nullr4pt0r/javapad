package live.javapad.v0.observer;

import live.javapad.v0.CollaborativeDocument;

/**
 * Observer interface for document updates.
 * Implements the Observer pattern for real-time document change notifications.
 */
public interface DocumentObserver {
    /**
     * Called when a document is updated.
     *
     * @param docId    The ID of the updated document
     * @param document The updated document instance
     */
    void onDocumentUpdate(String docId, CollaborativeDocument document);

    /**
     * Called when a new document is created.
     *
     * @param docId    The ID of the newly created document
     * @param document The created document instance
     */
    void onDocumentCreated(String docId, CollaborativeDocument document);

    /**
     * Called when a document is deleted.
     *
     * @param docId The ID of the deleted document
     */
    void onDocumentDeleted(String docId);
}