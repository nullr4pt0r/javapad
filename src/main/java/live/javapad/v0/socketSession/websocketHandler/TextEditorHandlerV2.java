package live.javapad.v0.socketSession.websocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TextEditorHandlerV2 extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); //store sessions

    private final ConcurrentHashMap<String, String> sessionToDocument = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("documentServiceV2")
    private DocumentServiceV2 documentService;
    @Autowired
    private ObjectMapper objectMapper;
    //all session comes here


    //session intiation

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws IOException {
        sessions.put(webSocketSession.getId(), webSocketSession);
        log.info("Session Initiated : {}", webSocketSession.getId());
        webSocketSession.sendMessage(new TextMessage("Session Connected:["+webSocketSession.getId()+"]"));
    }


    public void handleOperations(WebSocketSession webSocketSession, OperationRequest or) throws IOException {

        String documentId = or.getDocId();
        String operation = or.getEvent();
        String content = or.getData();
        String jsonResponse = null;

        try {
           switch(operation){
               case "create":
                   CollaborativeDocument cd = documentService.createDocument(or);
                   sessionToDocument.put(webSocketSession.getId(), cd.getId());
                   jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(cd.getId())
                           .sessionId(webSocketSession.getId()).event("create").data(content).cursorPosition(content.length()+1).
                           docVersion(cd.getVersion()).build());
                   webSocketSession.sendMessage(new TextMessage(jsonResponse));
                   break;
               case "open":
                   CollaborativeDocument doc = documentService.fetchAndAddCollaborator(documentId, or.getSessionId());
                   sessionToDocument.put(webSocketSession.getId(), documentId);
                   jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(documentId)
                           .sessionId(webSocketSession.getId()).event("open").data(doc.getContent()).cursorPosition(doc.getContent().length()+1)
                           .docVersion(doc.getVersion()).build());
                   webSocketSession.sendMessage(new TextMessage(jsonResponse));
                   break;
               case "insert" :
                   case "delete":
                       documentService.updateDocument(or);
                       broadcastDocumentToAll(documentId);
                       break;
           }
        }catch (IOException e) {
            webSocketSession.sendMessage(new TextMessage("Error : "+e.getMessage()));
        }
    }


    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws IOException {
        System.out.println(message.getPayload());

        OperationRequest or = objectMapper.readValue(message.getPayload(), OperationRequest.class);

        handleOperations(webSocketSession, or);
    }

    public void broadcastDocumentToAll(String documentId) throws IOException {
        CollaborativeDocument cd = documentService.getDocument(documentId);

        for(String sessionId: documentService.getCollaboratorsDetails(documentId)){
            WebSocketSession session = sessions.get(sessionId);
            Response response = Response.builder().docId(documentId).sessionId(session.getId()).event("update").data(cd.getContent())
                    .docVersion(cd.getVersion())
                    .cursorPosition(cd.getContent().length()+1).build();
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
            log.info("Send : {} to session : {}", cd.getContent(), sessionId);
        }

        log.info("Broadcast completed !");
    }




    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        String sessionId = session.getId();
        if(sessionToDocument.containsKey(sessionId)){
            String documentId = sessionToDocument.get(sessionId);
            sessionToDocument.remove(sessionId);
            documentService.removeDocument(documentId);
            log.info("Document removed for session : {}", sessionId);
        }
        sessions.remove(sessionId);
        log.info("Session disconnected : {}", sessionId);
    }
}
