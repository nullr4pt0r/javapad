package live.javapad.v0.document.service;

import live.javapad.v0.CollaborativeDocument;
import live.javapad.v0.datastore.DocumentStore;
import live.javapad.v0.dto.OperationRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Service("documentServiceV2")
@Slf4j
public class DocumentServiceV2 implements IDocumentService {

    private final DocumentStore documentStore;

    public CollaborativeDocument createDocument( OperationRequest operationRequest){
        String docId = String.valueOf(UUID.randomUUID());
        CollaborativeDocument cd = createCollaborativeDocument(operationRequest, docId);
        documentStore.addDocument(docId, cd);
        return cd;
    }

    public void updateDocument(OperationRequest or){
        documentStore.verifyDocumentExistence(or.getDocId());
        handleTransformation(or);
    }

    public CollaborativeDocument getDocument(String documentId){
        documentStore.verifyDocumentExistence(documentId);
        return documentStore.getDocumentByKey(documentId);
    }

    public CollaborativeDocument fetchAndAddCollaborator(String documentId, String sessionId){
        documentStore.verifyDocumentExistence(documentId);
        if(sessionId != null)
            addCollaborator(documentId, sessionId);
        return documentStore.getDocumentByKey(documentId);
    }

    public void addCollaborator(String documentId, String sessionId){
        documentStore.getDocumentByKey(documentId).getCollaborators().add(sessionId);
    }

    private CollaborativeDocument createCollaborativeDocument(OperationRequest operationRequest, String docId){

        CollaborativeDocument collaborativeDocument = new CollaborativeDocument();
        collaborativeDocument.setId(docId);
        collaborativeDocument.setContent(operationRequest.getData());
        collaborativeDocument.getCollaborators().add(operationRequest.getSessionId());
        collaborativeDocument.getHistory().add(operationRequest);
        return collaborativeDocument;
    }

    public List<String> getCollaboratorsDetails(String documentId){
        return documentStore.getDocumentByKey(documentId).getCollaborators();
    }

    public void removeDocument(String documentId){
        documentStore.removeDocument(documentId);
    }

    private void transformDocumentContent(OperationRequest operationRequest){
        CollaborativeDocument doc = documentStore.getDocumentByKey(operationRequest.getDocId());
        if(doc == null){
            return;
        }
            //just go with it
            StringBuilder docContent = new StringBuilder(doc.getContent());
            if(operationRequest.getEvent().equals("insert")){
              int pos = operationRequest.getCursorPosition();
              if(pos > docContent.length()){
                  pos = docContent.length();
              }
                docContent.insert(pos,operationRequest.getData());
            }else if(operationRequest.getEvent().equals("delete")){
              if(operationRequest.getCursorPosition() <= operationRequest.getEndCursorPosition()){
                  docContent.delete(operationRequest.getCursorPosition(), operationRequest.getEndCursorPosition()+1);
              }
            }
            doc.setContent(docContent.toString());
        doc.setVersion(doc.getVersion()+1);
        doc.getHistory().add(operationRequest);
        documentStore.addDocument(operationRequest.getDocId(), doc);
    }

    CollaborativeDocument transformDocumentOnVersionDifference(OperationRequest previous, OperationRequest current){
        String previousOperation = previous.getEvent();
        String currentOperation = current.getEvent();
        CollaborativeDocument doc = documentStore.getDocumentByKey(current.getDocId());
        StringBuilder docContent = new StringBuilder(doc.getContent());
        if(previousOperation.equals("insert") && currentOperation.equals("insert")){
            if(current.getCursorPosition() >= previous.getCursorPosition()){
                current.setCursorPosition(current.getCursorPosition()+previous.getData().length());
            }
        }else if(previousOperation.equals("insert") && currentOperation.equals("delete")){
            if(previous.getCursorPosition() < current.getCursorPosition()){
                int length = previous.getData().length();
                current.setCursorPosition(current.getCursorPosition()+length);
                current.setEndCursorPosition(current.getEndCursorPosition()+length);
            }
        }else if(previousOperation.equals("delete") && currentOperation.equals("insert")){
            int length = previous.getEndCursorPosition() - previous.getCursorPosition();
            if(previous.getEndCursorPosition()<current.getCursorPosition()){
                current.setCursorPosition(current.getCursorPosition()-length);
            }else if(current.getCursorPosition() >= previous.getCursorPosition()
                    && current.getCursorPosition() <= previous.getEndCursorPosition()){
                current.setCursorPosition(previous.getCursorPosition());
            }

        }else if(previousOperation.equals("delete") && currentOperation.equals("delete")){
            int p1 = previous.getCursorPosition();
            int p2 = previous.getEndCursorPosition();
            int c1 = current.getCursorPosition();
            int c2 = current.getEndCursorPosition();

            if(p2 < c1){
                current.setCursorPosition(c1 - (p2-p1));
                current.setEndCursorPosition(c2 - (p2-p1));
            }
           else if(c1 >= p1 && c2 <= p2){
                current.setCursorPosition(p1);
                current.setEndCursorPosition(p1);
            }
           else{
                int newStart = Math.max(c1, p2+1);
                current.setEndCursorPosition(newStart);
            }
            System.out.printf("Adjusted delete: [%d - %d]%n", current.getCursorPosition(), current.getEndCursorPosition());
        }
        transformDocumentContent(current);
        return doc;
    }

    void handleTransformation(OperationRequest request){
        CollaborativeDocument document = documentStore.getDocumentByKey(request.getDocId());
        Integer currentDocumentVersion = document.getVersion();
        if(currentDocumentVersion > request.getDocVersion()){
            OperationRequest previous = document.getHistory().get(document.getHistory().size()-1);
            transformDocumentOnVersionDifference(previous, request);
        }else{
            transformDocumentContent(request);
        }
    }

    // Enhanced methods to support Command pattern

    /**
     * Create a document with a specific ID and initial content.
     * Used by CreateDocumentCommand.
     *
     * @param documentId      The document ID
     * @param initialContent  The initial content
     * @param sessionId       The session ID of the creator
     * @return The created document
     */
    public CollaborativeDocument createDocumentWithId(String documentId, String initialContent, String sessionId) {
        CollaborativeDocument document = new CollaborativeDocument();
        document.setId(documentId);
        document.setContent(initialContent != null ? initialContent : "");
        document.getCollaborators().add(sessionId);
        
        // Create initial operation request for history
        OperationRequest initialOperation = new OperationRequest();
        initialOperation.setDocId(documentId);
        initialOperation.setSessionId(sessionId);
        initialOperation.setEvent("create");
        initialOperation.setData(initialContent != null ? initialContent : "");
        initialOperation.setCursorPosition(0);
        initialOperation.setDocVersion(0);
        
        document.getHistory().add(initialOperation);
        documentStore.addDocument(documentId, document);
        
        log.debug("Created document with ID: {} for session: {}", documentId, sessionId);
        return document;
    }

    /**
     * Insert text at a specific position in a document.
     * Used by InsertTextCommand.
     *
     * @param documentId The document ID
     * @param text       The text to insert
     * @param position   The position to insert at
     */
    public void insertText(String documentId, String text, int position) {
        documentStore.verifyDocumentExistence(documentId);
        CollaborativeDocument document = documentStore.getDocumentByKey(documentId);
        
        StringBuilder content = new StringBuilder(document.getContent());
        int insertPos = Math.max(0, Math.min(position, content.length()));
        content.insert(insertPos, text);
        
        document.setContent(content.toString());
        document.setVersion(document.getVersion() + 1);
        
        documentStore.addDocument(documentId, document);
        log.debug("Inserted '{}' at position {} in document: {}", text, position, documentId);
    }

    /**
     * Delete text from a document between specified positions.
     * Used by DeleteTextCommand.
     *
     * @param documentId    The document ID
     * @param startPosition The start position (inclusive)
     * @param endPosition   The end position (exclusive)
     */
    public void deleteText(String documentId, int startPosition, int endPosition) {
        documentStore.verifyDocumentExistence(documentId);
        CollaborativeDocument document = documentStore.getDocumentByKey(documentId);
        
        StringBuilder content = new StringBuilder(document.getContent());
        int start = Math.max(0, Math.min(startPosition, content.length()));
        int end = Math.max(start, Math.min(endPosition, content.length()));
        
        if (start < end) {
            content.delete(start, end);
            document.setContent(content.toString());
            document.setVersion(document.getVersion() + 1);
            documentStore.addDocument(documentId, document);
            log.debug("Deleted text from position {}-{} in document: {}", start, end, documentId);
        }
    }

    /**
     * Delete a document completely.
     * Used by CreateDocumentCommand undo.
     *
     * @param documentId The document ID
     */
    public void deleteDocument(String documentId) {
        documentStore.verifyDocumentExistence(documentId);
        documentStore.removeDocument(documentId);
        log.debug("Deleted document: {}", documentId);
    }
}
