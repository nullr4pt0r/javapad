package live.javapad.v0.observer;

import live.javapad.v0.CollaborativeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Publisher for document events implementing the Observer pattern.
 * Manages observers and notifies them asynchronously about document changes.
 */
@Component
@Slf4j
public class DocumentEventPublisher {
    
    private final CopyOnWriteArrayList<DocumentObserver> observers = new CopyOnWriteArrayList<>();
    private final ExecutorService notificationExecutor = Executors.newCachedThreadPool();

    /**
     * Add an observer to receive document update notifications.
     *
     * @param observer The observer to add
     */
    public void addObserver(DocumentObserver observer) {
        observers.add(observer);
        log.debug("Added document observer: {}", observer.getClass().getSimpleName());
    }

    /**
     * Remove an observer from receiving notifications.
     *
     * @param observer The observer to remove
     */
    public void removeObserver(DocumentObserver observer) {
        observers.remove(observer);
        log.debug("Removed document observer: {}", observer.getClass().getSimpleName());
    }

    /**
     * Notify all observers about a document update asynchronously.
     *
     * @param docId    The ID of the updated document
     * @param document The updated document instance
     */
    public void notifyDocumentUpdate(String docId, CollaborativeDocument document) {
        log.debug("Notifying {} observers about document update: {}", observers.size(), docId);
        
        observers.forEach(observer -> 
            CompletableFuture.runAsync(() -> {
                try {
                    observer.onDocumentUpdate(docId, document);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about document update: {}", 
                             observer.getClass().getSimpleName(), e.getMessage(), e);
                }
            }, notificationExecutor)
        );
    }

    /**
     * Notify all observers about a document creation asynchronously.
     *
     * @param docId    The ID of the newly created document
     * @param document The created document instance
     */
    public void notifyDocumentCreated(String docId, CollaborativeDocument document) {
        log.debug("Notifying {} observers about document creation: {}", observers.size(), docId);
        
        observers.forEach(observer -> 
            CompletableFuture.runAsync(() -> {
                try {
                    observer.onDocumentCreated(docId, document);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about document creation: {}", 
                             observer.getClass().getSimpleName(), e.getMessage(), e);
                }
            }, notificationExecutor)
        );
    }

    /**
     * Notify all observers about a document deletion asynchronously.
     *
     * @param docId The ID of the deleted document
     */
    public void notifyDocumentDeleted(String docId) {
        log.debug("Notifying {} observers about document deletion: {}", observers.size(), docId);
        
        observers.forEach(observer -> 
            CompletableFuture.runAsync(() -> {
                try {
                    observer.onDocumentDeleted(docId);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about document deletion: {}", 
                             observer.getClass().getSimpleName(), e.getMessage(), e);
                }
            }, notificationExecutor)
        );
    }

    /**
     * Get the number of registered observers.
     *
     * @return The count of observers
     */
    public int getObserverCount() {
        return observers.size();
    }
}