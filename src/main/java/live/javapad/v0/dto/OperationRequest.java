package live.javapad.v0.dto;

import lombok.Data;

@Data
public class OperationRequest {
    String data;
    Integer cursorPosition;
    Integer endCursorPosition;
    String docId;
    Integer docVersion;
    String sessionId;
    String event;
    String sessionName;
}
