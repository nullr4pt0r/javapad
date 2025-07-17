package live.javapad.v0.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class UIOpener {

    @Value("${server.port:8080}")
    private String serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserAfterStartup() {
        String url = "http://localhost:" + serverPort;
        log.info("Opening browser at: {}", url);
        
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                log.info("Could not open browser automatically. Please open manually at: {}", url);
                
                // Try platform-specific commands as fallback
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    // For Windows
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    // For Mac
                    Runtime.getRuntime().exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    // For Linux
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("Failed to open browser: {}", e.getMessage());
            log.info("Please open the application manually at: {}", url);
        }
    }
}