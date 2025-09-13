package live.javapad.v0.datastore;

import live.javapad.v0.CollaborativeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentStore
 * Tests document storage and retrieval functionality
 */
class DocumentStoreTest {

    private DocumentStore documentStore;
    private CollaborativeDocument testDocument;

    @BeforeEach
    void setUp() {
        documentStore = new DocumentStore();
        testDocument = CollaborativeDocument.builder()
                .id("doc-123")
                .content("Test document content")
                .version(1)
                .build();
    }

    @Test
    void addDocument_ShouldStoreDocument() {
        // When
        documentStore.addDocument("doc-123", testDocument);

        // Then
        CollaborativeDocument retrieved = documentStore.getDocumentByKey("doc-123");
        assertEquals(testDocument, retrieved);
    }

    @Test
    void getDocumentByKey_WithValidKey_ShouldReturnDocument() {
        // Given
        documentStore.addDocument("doc-123", testDocument);

        // When
        CollaborativeDocument result = documentStore.getDocumentByKey("doc-123");

        // Then
        assertEquals(testDocument, result);
        assertEquals("Test document content", result.getContent());
        assertEquals(1, result.getVersion().intValue());
    }

    @Test
    void getDocumentByKey_WithInvalidKey_ShouldThrowException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentStore.getDocumentByKey("invalid-key");
        });
        assertEquals("Document Not Found!", exception.getMessage());
    }

    @Test
    void getDocumentByKey_WithNullKey_ShouldThrowException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentStore.getDocumentByKey(null);
        });
        assertEquals("Key is invalid", exception.getMessage());
    }

    @Test
    void verifyDocumentExistence_WithExistingDocument_ShouldNotThrow() {
        // Given
        documentStore.addDocument("doc-123", testDocument);

        // When & Then
        assertDoesNotThrow(() -> {
            documentStore.verifyDocumentExistence("doc-123");
        });
    }

    @Test
    void verifyDocumentExistence_WithNonExistentDocument_ShouldThrowException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentStore.verifyDocumentExistence("non-existent");
        });
        assertEquals("Document Not Found!", exception.getMessage());
    }

    @Test
    void removeDocument_WithExistingDocument_ShouldRemoveDocument() {
        // Given
        documentStore.addDocument("doc-123", testDocument);

        // When
        documentStore.removeDocument("doc-123");

        // Then
        assertThrows(RuntimeException.class, () -> {
            documentStore.getDocumentByKey("doc-123");
        });
    }

    @Test
    void removeDocument_WithNonExistentDocument_ShouldThrowException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentStore.removeDocument("non-existent");
        });
        assertEquals("Document Not Found!", exception.getMessage());
    }

    @Test
    void addDocument_ShouldOverwriteExistingDocument() {
        // Given
        documentStore.addDocument("doc-123", testDocument);
        
        CollaborativeDocument newDocument = CollaborativeDocument.builder()
                .id("doc-123")
                .content("Updated content")
                .version(2)
                .build();

        // When
        documentStore.addDocument("doc-123", newDocument);

        // Then
        CollaborativeDocument retrieved = documentStore.getDocumentByKey("doc-123");
        assertEquals("Updated content", retrieved.getContent());
        assertEquals(2, retrieved.getVersion().intValue());
    }

    @Test
    void multipleDocuments_ShouldBeStoredIndependently() {
        // Given
        CollaborativeDocument doc1 = CollaborativeDocument.builder()
                .id("doc-1")
                .content("Content 1")
                .version(1)
                .build();
                
        CollaborativeDocument doc2 = CollaborativeDocument.builder()
                .id("doc-2")
                .content("Content 2")
                .version(1)
                .build();

        // When
        documentStore.addDocument("doc-1", doc1);
        documentStore.addDocument("doc-2", doc2);

        // Then
        assertEquals("Content 1", documentStore.getDocumentByKey("doc-1").getContent());
        assertEquals("Content 2", documentStore.getDocumentByKey("doc-2").getContent());
    }
}