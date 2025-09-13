package live.javapad.v0.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentService
 * Tests basic document operations (create, update, retrieve)
 */
class DocumentServiceTest {

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService();
    }

    @Test
    void createDocument_ShouldReturnUUID() {
        // Given
        String content = "Test document content";

        // When
        String documentId = documentService.createDocument(content);

        // Then
        assertNotNull(documentId);
        assertFalse(documentId.isEmpty());
        // Verify UUID format (36 characters with dashes)
        assertEquals(36, documentId.length());
        assertTrue(documentId.contains("-"));
    }

    @Test
    void createDocument_ShouldStoreContent() {
        // Given
        String content = "Test document content";

        // When
        String documentId = documentService.createDocument(content);
        String retrievedContent = documentService.getDocument(documentId);

        // Then
        assertEquals(content, retrievedContent);
    }

    @Test
    void getDocument_WithValidId_ShouldReturnContent() {
        // Given
        String content = "Another test document";
        String documentId = documentService.createDocument(content);

        // When
        String retrievedContent = documentService.getDocument(documentId);

        // Then
        assertEquals(content, retrievedContent);
    }

    @Test
    void getDocument_WithInvalidId_ShouldThrowException() {
        // Given
        String invalidId = "non-existent-id";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentService.getDocument(invalidId);
        });
        assertEquals("Document Not Found!", exception.getMessage());
    }

    @Test
    void updateDocument_WithValidId_ShouldUpdateContent() {
        // Given
        String originalContent = "Original content";
        String updatedContent = "Updated content";
        String documentId = documentService.createDocument(originalContent);

        // When
        documentService.updateDocument(documentId, updatedContent);
        String retrievedContent = documentService.getDocument(documentId);

        // Then
        assertEquals(updatedContent, retrievedContent);
        assertNotEquals(originalContent, retrievedContent);
    }

    @Test
    void updateDocument_WithInvalidId_ShouldThrowException() {
        // Given
        String invalidId = "non-existent-id";
        String content = "Some content";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            documentService.updateDocument(invalidId, content);
        });
        assertEquals("Document Not Found!", exception.getMessage());
    }

    @Test
    void createDocument_WithEmptyContent_ShouldWork() {
        // Given
        String emptyContent = "";

        // When
        String documentId = documentService.createDocument(emptyContent);
        String retrievedContent = documentService.getDocument(documentId);

        // Then
        assertEquals(emptyContent, retrievedContent);
    }

    @Test
    void multipleDocuments_ShouldBeIndependent() {
        // Given
        String content1 = "Document 1 content";
        String content2 = "Document 2 content";

        // When
        String docId1 = documentService.createDocument(content1);
        String docId2 = documentService.createDocument(content2);

        // Then
        assertNotEquals(docId1, docId2);
        assertEquals(content1, documentService.getDocument(docId1));
        assertEquals(content2, documentService.getDocument(docId2));
    }
}