package live.javapad.v0.socketSession.websocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import live.javapad.v0.document.service.DocumentService;
import live.javapad.v0.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TextEditorHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> documentToSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToDocument = new ConcurrentHashMap<>();

    @Autowired
    private DocumentService documentService;
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



    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws IOException {
        System.out.println(message.getPayload());

        String documentId = sessionToDocument.get(webSocketSession.getId());
        String[] msg = message.getPayload().split(":",2);
        String command = msg[0];
        String content = msg[1].substring(1, msg[1].length()-1);
        String jsonResponse = null;
        try {
            switch (command){
                case "create":
                    documentId = documentService.createDocument(content);
                    sessionToDocument.put(webSocketSession.getId(), documentId);
                    documentToSessions.computeIfAbsent(documentId, (k) -> new ArrayList<>()).add(webSocketSession.getId());
                    jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(documentId)
                            .sessionId(webSocketSession.getId()).event("create").data(content).cursorPosition(content.length()+1).build());
                    webSocketSession.sendMessage(new TextMessage(jsonResponse));
                    break;
                case "update":
                    String[] updateContent = msg[1].split(",",2);
                    documentId = updateContent[0].substring(1, updateContent[0].length());
                    content = updateContent[1].substring(0, updateContent[1].length()-1);
                    documentService.updateDocument(documentId, content);
                    broadcastDocumentToAll(documentId);
                    break;
                case "open":
                    documentId = content;
                    String documentContent = documentService.getDocument(documentId);
                    documentToSessions.computeIfAbsent(documentId,(k) -> new ArrayList<>()).add(webSocketSession.getId());
                    jsonResponse = objectMapper.writeValueAsString(Response.builder().docId(documentId)
                            .sessionId(webSocketSession.getId()).event("open").data(documentContent).cursorPosition(documentContent.length()+1).build());
                    webSocketSession.sendMessage(new TextMessage(jsonResponse));
                    break;

            }
        } catch (IOException e) {
            webSocketSession.sendMessage(new TextMessage("Error : "+e.getMessage()));
        }
    }

    public void broadcastDocumentToAll(String documentId) throws IOException {
        String content = documentService.getDocument(documentId);

        for(String sessionId: documentToSessions.get(documentId)){
            WebSocketSession session = sessions.get(sessionId);
            Response response = Response.builder().docId(documentId).sessionId(session.getId()).event("update").data(content)
                    .cursorPosition(content.length()+1).build();
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
            log.info("Send : {} to session : {}", content, sessionId);
        }

        log.info("Broadcast completed !");
    }




    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        String sessionId = session.getId();
        if(sessionToDocument.containsKey(sessionId)){
            String documentId = sessionToDocument.get(sessionId);
            sessionToDocument.remove(sessionId);
            documentToSessions.remove(documentId);
            log.info("Document removed for session : {}", sessionId);
        }
        sessions.remove(sessionId);
        log.info("Session disconnected : {}", sessionId);
    }
}
