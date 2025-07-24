package live.javapad.v0.datastore;

import live.javapad.v0.CollaborativeDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Component
public final class DocumentStore {
    private static final ConcurrentHashMap<String, CollaborativeDocument> documents = new ConcurrentHashMap<>();

   public void addDocument(String key, CollaborativeDocument doc){
       documents.put(key, doc);
   }

   public CollaborativeDocument getDocumentByKey(String key){
       if(key != null) {
           verifyDocumentExistence(key);
           return documents.get(key);
       }
       throw new RuntimeException("Key is invalid");
   }

    public void verifyDocumentExistence(String documentId) {
        if(!documents.containsKey(documentId)){
            throw new RuntimeException("Document Not Found!");
        }
    }

    public void removeDocument(String key){
       verifyDocumentExistence(key);
       documents.remove(key);
    }
}
