package live.javapad.v0.command;

/**
 * Command interface implementing the Command pattern.
 * Supports operation queuing and undo/redo functionality.
 */
public interface Command {
    
    /**
     * Execute the command.
     */
    void execute();

    /**
     * Undo the command execution.
     */
    void undo();

    /**
     * Get the document ID associated with this command.
     *
     * @return The document ID
     */
    String getDocumentId();

    /**
     * Get the session ID associated with this command.
     *
     * @return The session ID
     */
    String getSessionId();

    /**
     * Get a description of this command for logging/debugging.
     *
     * @return Command description
     */
    String getDescription();
}