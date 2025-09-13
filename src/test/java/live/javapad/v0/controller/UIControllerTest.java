package live.javapad.v0.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UIController
 * Tests the UI routing and redirection functionality
 */
@WebMvcTest(UIController.class)
class UIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void index_ShouldForwardToIndexHtml() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void openUI_ShouldRedirectToRoot() throws Exception {
        // When & Then
        mockMvc.perform(get("/open-ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}