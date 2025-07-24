package live.javapad.v0.datastore;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public final class SessionStore {
    private static final ConcurrentHashMap<String, String> sessionToDocument = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addNewSession(String key, WebSocketSession session){
        sessions.put(key, session);
    }

    public WebSocketSession getSessionBySessionId(String key){
        if(key != null){
            return sessions.get(key);
        }
        throw new RuntimeException("Key is invalid");
    }

    public void removeSession(String key){
        if(key != null){
            sessions.remove(key);
        }
        throw new RuntimeException("Key is invalid");
    }
    public void addDocumentToSession(String sessionId, String docId){
        sessionToDocument.put(sessionId, docId);
    }

    public String getDocumentIdBySessionId(String sessionId){
        if(sessionId != null){
            return sessionToDocument.get(sessionId);
        }
        throw new RuntimeException("Key is invalid");
    }

    public boolean verifySessionExistence(String sessionId){
        return sessionToDocument.containsKey(sessionId);
    }

    public void removeDocumentId(String key){
        if(key != null){
            sessionToDocument.remove(key);
        }
        throw new RuntimeException("Key is invalid");
    }

}
