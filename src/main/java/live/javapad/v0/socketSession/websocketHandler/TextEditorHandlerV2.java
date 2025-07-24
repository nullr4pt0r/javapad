package live.javapad.v0.socketSession.websocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.javapad.v0.datastore.SessionStore;
import live.javapad.v0.document.service.DocumentServiceV2;
import live.javapad.v0.dto.OperationRequest;
import live.javapad.v0.operationStrategy.*;
import live.javapad.v0.session.service.SessionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@AllArgsConstructor
@Component
@Slf4j
public class TextEditorHandlerV2 extends TextWebSocketHandler {




    @Qualifier("documentServiceV2")
    private final DocumentServiceV2 documentService;
    @Autowired
    private ObjectMapper objectMapper;


    private final OperationStrategyFactory operationStrategyFactory;
    private final SessionService sessionService;
    private final SessionStore sessionStore;


    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws IOException {
        sessionStore.addNewSession(webSocketSession.getId(), webSocketSession);
        log.info("Session Initiated : {}", webSocketSession.getId());
        webSocketSession.sendMessage(new TextMessage("Session Connected:["+webSocketSession.getId()+"]"));
    }


    public void handleOperations(WebSocketSession webSocketSession, OperationRequest or) throws IOException {

        String operation = or.getEvent();

        try {
            OperationStrategy operationStrategy = operationStrategyFactory.getOperationStrategy(operation);
            operationStrategy.apply(or, webSocketSession);
        }catch (Exception e) {
            webSocketSession.sendMessage(new TextMessage("Error : "+e.getMessage()));
        }
    }

//    private void broadcastCursorPosition(OperationRequest cursorUpdate, String senderSessionId) {
//        try {
//          String json = objectMapper.writeValueAsString(cursorUpdate);
//            sessions.forEach((id, sess) -> {
//                if (!id.equals(senderSessionId) && sess.isOpen()) {
//                    try {
//                        sess.sendMessage(new TextMessage(json));
//                    } catch (IOException e) {
//                        log.error("Failed to send cursor to session {}", id, e);
//                    }
//                }
//            });
//        } catch (JsonProcessingException e) {
//            log.error("Failed to serialize cursor position", e);
//            return;
//        }
//
//    }


    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws IOException {
        System.out.println(message.getPayload());

        OperationRequest or = objectMapper.readValue(message.getPayload(), OperationRequest.class);

        if ("cursor".equals(or.getEvent())) {
//            broadcastCursorPosition(or, or.getSessionId());
            return;
        }

        handleOperations(webSocketSession, or);
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        sessionService.removeDocuments(session);
        log.info("Session disconnected : {}", session.getId());
    }
}
