package live.javapad.v0.operationStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.dto.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateOperationStrategy
 * Tests document creation functionality via WebSocket operations
 */
@ExtendWith(MockitoExtension.class)
class CreateOperationStrategyTest {

    @Mock
    private DocumentServiceV2 documentService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SessionStore sessionStore;

    @Mock
    private WebSocketSession webSocketSession;

    private CreateOperationStrategy createOperationStrategy;

    private OperationRequest operationRequest;
    private CollaborativeDocument collaborativeDocument;

    @BeforeEach
    void setUp() {
        createOperationStrategy = new CreateOperationStrategy(documentService, objectMapper, sessionStore);

        operationRequest = new OperationRequest();
        operationRequest.setData("Test document content");
        operationRequest.setSessionId("session-123");
        operationRequest.setEvent("create");

        collaborativeDocument = CollaborativeDocument.builder()
                .id("doc-456")
                .content("Test document content")
                .version(0)
                .build();
    }

    @Test
    void apply_ShouldCreateDocumentAndSendResponse() throws Exception {
        // Given
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        
        Response expectedResponse = Response.builder()
                .docId("doc-456")
                .sessionId("session-123")
                .event("create")
                .data("Test document content")
                .cursorPosition(21)
                .docVersion(0)
                .build();
        
        String expectedJson = "{\"docId\":\"doc-456\",\"sessionId\":\"session-123\"}";
        when(objectMapper.writeValueAsString(any(Response.class))).thenReturn(expectedJson);

        // When
        createOperationStrategy.apply(operationRequest, webSocketSession);

        // Then
        verify(documentService).createDocument(operationRequest);
        verify(sessionStore).addDocumentToSession("session-123", "doc-456");
        verify(webSocketSession).sendMessage(any(TextMessage.class));
        verify(objectMapper).writeValueAsString(any(Response.class));
    }

    @Test
    void apply_ShouldCalculateCorrectCursorPosition() throws Exception {
        // Given
        String content = "Hello World";
        operationRequest.setData(content);
        collaborativeDocument.setContent(content);
        
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any(Response.class))).thenReturn("{}");

        // When
        createOperationStrategy.apply(operationRequest, webSocketSession);

        // Then
        verify(objectMapper).writeValueAsString(argThat(response -> {
            Response r = (Response) response;
            return r.getCursorPosition().equals(content.length() + 1);
        }));
    }

    @Test
    void apply_WithEmptyContent_ShouldWork() throws Exception {
        // Given
        operationRequest.setData("");
        collaborativeDocument.setContent("");
        
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any(Response.class))).thenReturn("{}");

        // When
        createOperationStrategy.apply(operationRequest, webSocketSession);

        // Then
        verify(documentService).createDocument(operationRequest);
        verify(sessionStore).addDocumentToSession("session-123", "doc-456");
        verify(webSocketSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void apply_WithJsonProcessingException_ShouldThrowRuntimeException() throws Exception {
        // Given
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any(Response.class)))
                .thenThrow(new JsonProcessingException("JSON error") {});

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            createOperationStrategy.apply(operationRequest, webSocketSession);
        });
        
        assertTrue(exception.getCause() instanceof JsonProcessingException);
        verify(documentService).createDocument(operationRequest);
        verify(sessionStore).addDocumentToSession("session-123", "doc-456");
    }

    @Test
    void apply_WithIOException_ShouldThrowRuntimeException() throws Exception {
        // Given
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any(Response.class))).thenReturn("{}");
        doThrow(new IOException("WebSocket error")).when(webSocketSession).sendMessage(any(TextMessage.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            createOperationStrategy.apply(operationRequest, webSocketSession);
        });
        
        assertTrue(exception.getCause() instanceof IOException);
        verify(documentService).createDocument(operationRequest);
        verify(sessionStore).addDocumentToSession("session-123", "doc-456");
    }

    @Test
    void apply_ShouldBuildCorrectResponse() throws Exception {
        // Given
        when(documentService.createDocument(operationRequest)).thenReturn(collaborativeDocument);
        when(webSocketSession.getId()).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any(Response.class))).thenReturn("{}");

        // When
        createOperationStrategy.apply(operationRequest, webSocketSession);

        // Then
        verify(objectMapper).writeValueAsString(argThat(response -> {
            Response r = (Response) response;
            return "doc-456".equals(r.getDocId()) &&
                   "session-123".equals(r.getSessionId()) &&
                   "create".equals(r.getEvent()) &&
                   "Test document content".equals(r.getData()) &&
                   r.getCursorPosition().equals(22) &&
                   r.getDocVersion().equals(0);
        }));
    }
}