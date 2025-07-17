package live.javapad.v0.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DocumentService {

    private final ConcurrentHashMap<String, String> documentMap = new ConcurrentHashMap<>();
    public String createDocument(String content){
        String uuid = String.valueOf(UUID.randomUUID());
        documentMap.put(uuid, content);
        return uuid;
    }

    public void updateDocument(String documentId, String content){
        verifyDocumentExistence(documentId);
        documentMap.put(documentId, content);
    }

    public String getDocument(String documentId){
        verifyDocumentExistence(documentId);
        return documentMap.get(documentId);
    }

    private void verifyDocumentExistence(String documentId) {
        if(!documentMap.containsKey(documentId)){
            throw new RuntimeException("Document Not Found!");
        }
    }
}
