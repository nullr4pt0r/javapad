package live.javapad.v0;

import live.javapad.v0.dto.OperationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollaborativeDocument {
    private String id;
    private String content;
    @Builder.Default
    private Integer version = 0;
    @Builder.Default
    private List<OperationRequest> history = new ArrayList<>();
    @Builder.Default
    private List<String> collaborators = new ArrayList<>();
}
