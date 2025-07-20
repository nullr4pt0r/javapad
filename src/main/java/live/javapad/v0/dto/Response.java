package live.javapad.v0.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Response {
    String event;
    String data;
    String sessionId;
    String docId;
    Integer cursorPosition;
    Integer docVersion;
}
