package live.javapad.v0.config;

import live.javapad.v0.command.CommandProcessor;
import live.javapad.v0.observer.DocumentEventPublisher;
import live.javapad.v0.observer.WebSocketNotificationObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to initialize design pattern components.
 * Sets up Observer pattern and Command pattern initialization.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DesignPatternsConfig implements CommandLineRunner {

    private final DocumentEventPublisher documentEventPublisher;
    private final WebSocketNotificationObserver webSocketNotificationObserver;
    private final CommandProcessor commandProcessor;

    @Override
    public void run(String... args) {
        // Register WebSocket observer with the publisher
        documentEventPublisher.addObserver(webSocketNotificationObserver);
        log.info("Registered WebSocket notification observer with document event publisher");

        // Initialize command processor
        commandProcessor.initialize();
        log.info("Initialized command processor for undo/redo functionality");

        log.info("Design patterns configuration completed successfully");
    }
}