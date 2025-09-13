package live.javapad.v0.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Stack;

/**
 * Command processor implementing the Command pattern.
 * Handles command queuing, execution, and undo/redo functionality.
 */
@Component
@Slf4j
public class CommandProcessor {
    
    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Per-document undo/redo stacks
    private final ConcurrentHashMap<String, Stack<Command>> undoStacks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Stack<Command>> redoStacks = new ConcurrentHashMap<>();
    
    private static final int MAX_UNDO_HISTORY = 50;

    /**
     * Initialize the command processor and start the processing thread.
     */
    public void initialize() {
        if (isProcessing.compareAndSet(false, true)) {
            commandExecutor.submit(this::processCommands);
            log.info("Command processor initialized and started");
        }
    }

    /**
     * Submit a command for execution.
     *
     * @param command The command to execute
     */
    public void submitCommand(Command command) {
        try {
            commandQueue.put(command);
            log.debug("Command submitted for execution: {}", command.getDescription());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while submitting command: {}", e.getMessage());
        }
    }

    /**
     * Undo the last command for a specific document.
     *
     * @param documentId The document ID
     * @return true if undo was successful, false otherwise
     */
    public boolean undoLastCommand(String documentId) {
        Stack<Command> undoStack = undoStacks.get(documentId);
        if (undoStack == null || undoStack.isEmpty()) {
            log.debug("No commands to undo for document: {}", documentId);
            return false;
        }

        try {
            Command lastCommand = undoStack.pop();
            lastCommand.undo();
            
            // Move to redo stack
            redoStacks.computeIfAbsent(documentId, k -> new Stack<>()).push(lastCommand);
            
            log.debug("Successfully undid command: {} for document: {}", 
                     lastCommand.getDescription(), documentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to undo command for document {}: {}", documentId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Redo the last undone command for a specific document.
     *
     * @param documentId The document ID
     * @return true if redo was successful, false otherwise
     */
    public boolean redoLastCommand(String documentId) {
        Stack<Command> redoStack = redoStacks.get(documentId);
        if (redoStack == null || redoStack.isEmpty()) {
            log.debug("No commands to redo for document: {}", documentId);
            return false;
        }

        try {
            Command lastUndoneCommand = redoStack.pop();
            lastUndoneCommand.execute();
            
            // Move back to undo stack
            undoStacks.computeIfAbsent(documentId, k -> new Stack<>()).push(lastUndoneCommand);
            
            log.debug("Successfully redid command: {} for document: {}", 
                     lastUndoneCommand.getDescription(), documentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to redo command for document {}: {}", documentId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the number of undoable commands for a document.
     *
     * @param documentId The document ID
     * @return Number of undoable commands
     */
    public int getUndoCount(String documentId) {
        Stack<Command> undoStack = undoStacks.get(documentId);
        return undoStack != null ? undoStack.size() : 0;
    }

    /**
     * Get the number of redoable commands for a document.
     *
     * @param documentId The document ID
     * @return Number of redoable commands
     */
    public int getRedoCount(String documentId) {
        Stack<Command> redoStack = redoStacks.get(documentId);
        return redoStack != null ? redoStack.size() : 0;
    }

    /**
     * Clear undo/redo history for a document.
     *
     * @param documentId The document ID
     */
    public void clearHistory(String documentId) {
        undoStacks.remove(documentId);
        redoStacks.remove(documentId);
        log.debug("Cleared command history for document: {}", documentId);
    }

    /**
     * Process commands from the queue continuously.
     */
    private void processCommands() {
        log.info("Command processing thread started");
        
        while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Command command = commandQueue.take();
                executeCommand(command);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Command processing thread interrupted");
                break;
            } catch (Exception e) {
                log.error("Error processing command: {}", e.getMessage(), e);
            }
        }
        
        log.info("Command processing thread stopped");
    }

    /**
     * Execute a command and manage undo/redo stacks.
     *
     * @param command The command to execute
     */
    private void executeCommand(Command command) {
        try {
            command.execute();
            
            // Add to undo stack
            String documentId = command.getDocumentId();
            Stack<Command> undoStack = undoStacks.computeIfAbsent(documentId, k -> new Stack<>());
            undoStack.push(command);
            
            // Limit undo history size
            if (undoStack.size() > MAX_UNDO_HISTORY) {
                undoStack.removeElementAt(0);
            }
            
            // Clear redo stack when new command is executed
            Stack<Command> redoStack = redoStacks.get(documentId);
            if (redoStack != null) {
                redoStack.clear();
            }
            
            log.debug("Successfully executed command: {} for document: {}", 
                     command.getDescription(), documentId);
                     
        } catch (Exception e) {
            log.error("Failed to execute command {}: {}", 
                     command.getDescription(), e.getMessage(), e);
        }
    }

    /**
     * Stop the command processor.
     */
    public void shutdown() {
        isProcessing.set(false);
        commandExecutor.shutdown();
        log.info("Command processor shutdown");
    }
}