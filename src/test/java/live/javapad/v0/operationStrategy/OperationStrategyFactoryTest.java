package live.javapad.v0.operationStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationStrategyFactory
 * Tests the factory pattern implementation for operation strategy selection
 */
@ExtendWith(MockitoExtension.class)
class OperationStrategyFactoryTest {

    @Mock
    private CreateOperationStrategy createOperationStrategy;

    @Mock
    private OpenOperationStrategy openOperationStrategy;

    @Mock
    private UpdateOperationStrategy updateOperationStrategy;

    private OperationStrategyFactory operationStrategyFactory;

    @BeforeEach
    void setUp() {
        operationStrategyFactory = new OperationStrategyFactory(
                createOperationStrategy,
                openOperationStrategy,
                updateOperationStrategy
        );
    }

    @Test
    void getOperationStrategy_WithCreateOperation_ShouldReturnCreateStrategy() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("create");

        // Then
        assertEquals(createOperationStrategy, result);
    }

    @Test
    void getOperationStrategy_WithOpenOperation_ShouldReturnOpenStrategy() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("open");

        // Then
        assertEquals(openOperationStrategy, result);
    }

    @Test
    void getOperationStrategy_WithInsertOperation_ShouldReturnUpdateStrategy() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("insert");

        // Then
        assertEquals(updateOperationStrategy, result);
    }

    @Test
    void getOperationStrategy_WithDeleteOperation_ShouldReturnUpdateStrategy() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("delete");

        // Then
        assertEquals(updateOperationStrategy, result);
    }

    @Test
    void getOperationStrategy_WithUnknownOperation_ShouldReturnNull() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("unknown");

        // Then
        assertNull(result);
    }

    @Test
    void getOperationStrategy_WithNullOperation_ShouldReturnNull() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy(null);

        // Then
        assertNull(result);
    }

    @Test
    void getOperationStrategy_WithEmptyOperation_ShouldReturnNull() {
        // When
        OperationStrategy result = operationStrategyFactory.getOperationStrategy("");

        // Then
        assertNull(result);
    }

    @Test
    void getOperationStrategy_WithCaseVariations_ShouldWorkCorrectly() {
        // Test that the factory is case-sensitive
        assertNull(operationStrategyFactory.getOperationStrategy("CREATE"));
        assertNull(operationStrategyFactory.getOperationStrategy("Create"));
        assertNull(operationStrategyFactory.getOperationStrategy("OPEN"));
        assertNull(operationStrategyFactory.getOperationStrategy("INSERT"));
        assertNull(operationStrategyFactory.getOperationStrategy("DELETE"));
    }
}