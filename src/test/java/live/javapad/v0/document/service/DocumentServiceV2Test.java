package live.javapad.v0.document.service;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.DocumentStore;
import live.javapad.v0.dto.OperationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentServiceV2
 * Tests advanced document operations with versioning and collaboration
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceV2Test {

    @Mock
    private DocumentStore documentStore;

    @InjectMocks
    private DocumentServiceV2 documentService;

    private OperationRequest createOperationRequest;
    private CollaborativeDocument mockDocument;

    @BeforeEach
    void setUp() {
        createOperationRequest = new OperationRequest();
        createOperationRequest.setData("Initial content");
        createOperationRequest.setSessionId("session-123");
        createOperationRequest.setEvent("create");

        mockDocument = CollaborativeDocument.builder()
                .id("doc-123")
                .content("Test content")
                .version(1)
                .build();
    }

    @Test
    void createDocument_ShouldCreateAndStoreDocument() {
        // When
        CollaborativeDocument result = documentService.createDocument(createOperationRequest);

        // Then
        assertNotNull(result);
        assertEquals("Initial content", result.getContent());
        assertEquals(0, result.getVersion().intValue());
        assertTrue(result.getCollaborators().contains("session-123"));
        assertTrue(result.getHistory().contains(createOperationRequest));
        
        verify(documentStore).addDocument(anyString(), any(CollaborativeDocument.class));
    }

    @Test
    void getDocument_ShouldReturnDocumentFromStore() {
        // Given
        String docId = "doc-123";
        when(documentStore.getDocumentByKey(docId)).thenReturn(mockDocument);

        // When
        CollaborativeDocument result = documentService.getDocument(docId);

        // Then
        assertEquals(mockDocument, result);
        verify(documentStore).verifyDocumentExistence(docId);
        verify(documentStore).getDocumentByKey(docId);
    }

    @Test
    void fetchAndAddCollaborator_ShouldAddCollaboratorToDocument() {
        // Given
        String docId = "doc-123";
        String sessionId = "session-456";
        when(documentStore.getDocumentByKey(docId)).thenReturn(mockDocument);

        // When
        CollaborativeDocument result = documentService.fetchAndAddCollaborator(docId, sessionId);

        // Then
        assertEquals(mockDocument, result);
        assertTrue(result.getCollaborators().contains(sessionId));
        verify(documentStore).verifyDocumentExistence(docId);
    }

    @Test
    void updateDocument_WithSameVersion_ShouldTransformDirectly() {
        // Given
        OperationRequest updateRequest = new OperationRequest();
        updateRequest.setDocId("doc-123");
        updateRequest.setEvent("insert");
        updateRequest.setData("Hello");
        updateRequest.setCursorPosition(0);
        updateRequest.setDocVersion(1);

        mockDocument.setVersion(1);
        when(documentStore.getDocumentByKey("doc-123")).thenReturn(mockDocument);

        // When
        documentService.updateDocument(updateRequest);

        // Then
        verify(documentStore).verifyDocumentExistence("doc-123");
        verify(documentStore, atLeastOnce()).getDocumentByKey("doc-123");
    }

    @Test
    void addCollaborator_ShouldAddSessionToCollaboratorsList() {
        // Given
        String docId = "doc-123";
        String sessionId = "new-session";
        when(documentStore.getDocumentByKey(docId)).thenReturn(mockDocument);

        // When
        documentService.addCollaborator(docId, sessionId);

        // Then
        assertTrue(mockDocument.getCollaborators().contains(sessionId));
    }

    @Test
    void getCollaboratorsDetails_ShouldReturnCollaboratorsList() {
        // Given
        String docId = "doc-123";
        mockDocument.getCollaborators().add("session-1");
        mockDocument.getCollaborators().add("session-2");
        when(documentStore.getDocumentByKey(docId)).thenReturn(mockDocument);

        // When
        List<String> collaborators = documentService.getCollaboratorsDetails(docId);

        // Then
        assertEquals(2, collaborators.size());
        assertTrue(collaborators.contains("session-1"));
        assertTrue(collaborators.contains("session-2"));
    }

    @Test
    void removeDocument_ShouldCallDocumentStore() {
        // Given
        String docId = "doc-123";

        // When
        documentService.removeDocument(docId);

        // Then
        verify(documentStore).removeDocument(docId);
    }

    @Test
    void insertOperation_ShouldUpdateContentAndVersion() {
        // Given
        OperationRequest insertRequest = new OperationRequest();
        insertRequest.setDocId("doc-123");
        insertRequest.setEvent("insert");
        insertRequest.setData("Hello ");
        insertRequest.setCursorPosition(0);
        insertRequest.setDocVersion(1);

        mockDocument.setContent("World");
        mockDocument.setVersion(1);
        when(documentStore.getDocumentByKey("doc-123")).thenReturn(mockDocument);

        // When
        documentService.updateDocument(insertRequest);

        // Then
        assertEquals("Hello World", mockDocument.getContent());
        assertEquals(2, mockDocument.getVersion().intValue());
        assertTrue(mockDocument.getHistory().contains(insertRequest));
    }

    @Test
    void deleteOperation_ShouldRemoveContentAndUpdateVersion() {
        // Given
        OperationRequest deleteRequest = new OperationRequest();
        deleteRequest.setDocId("doc-123");
        deleteRequest.setEvent("delete");
        deleteRequest.setCursorPosition(0);
        deleteRequest.setEndCursorPosition(4);
        deleteRequest.setDocVersion(1);

        mockDocument.setContent("Hello World");
        mockDocument.setVersion(1);
        when(documentStore.getDocumentByKey("doc-123")).thenReturn(mockDocument);

        // When
        documentService.updateDocument(deleteRequest);

        // Then
        assertEquals(" World", mockDocument.getContent());
        assertEquals(2, mockDocument.getVersion().intValue());
    }

    @Test
    void insertOperation_WithPositionBeyondLength_ShouldInsertAtEnd() {
        // Given
        OperationRequest insertRequest = new OperationRequest();
        insertRequest.setDocId("doc-123");
        insertRequest.setEvent("insert");
        insertRequest.setData("!");
        insertRequest.setCursorPosition(100); // Beyond content length
        insertRequest.setDocVersion(1);

        mockDocument.setContent("Hello");
        mockDocument.setVersion(1);
        when(documentStore.getDocumentByKey("doc-123")).thenReturn(mockDocument);

        // When
        documentService.updateDocument(insertRequest);

        // Then
        assertEquals("Hello!", mockDocument.getContent());
    }
}