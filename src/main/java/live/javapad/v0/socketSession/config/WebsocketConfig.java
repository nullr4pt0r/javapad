package live.javapad.v0.socketSession.config;

import live.javapad.v0.socketSession.websocketHandler.TextEditorHandler;
import live.javapad.v0.socketSession.websocketHandler.TextEditorHandlerV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;


@Component
@Slf4j
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {

    private final TextEditorHandlerV2 textEditorHandler;

    public WebsocketConfig(TextEditorHandlerV2 textEditorHandler){
        this.textEditorHandler = textEditorHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(textEditorHandler, "/editor")
                    .setAllowedOrigins("*");
    }
}
