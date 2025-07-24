package live.javapad.v0.operationStrategy;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class OperationStrategyFactory {

    private final CreateOperationStrategy createOperationStrategy;
    private final OpenOperationStrategy openOperationStrategy;
    private final UpdateOperationStrategy updateOperationStrategy;

    public OperationStrategy getOperationStrategy(String operation){
        switch (operation){
            case "create" :
                return createOperationStrategy;
            case "open":
                return openOperationStrategy;
            case "insert":
            case "delete":
                return updateOperationStrategy;
        }
        return null;
    }

}
