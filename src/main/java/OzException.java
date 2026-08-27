/**
 * Signals an error specific to the Oz chatbot application operations.
 */
public class OzException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an OzException with the specified error message.
     *
     * @param message Explanatory message detailing the cause of the error.
     */
    public OzException(String message) {
        super(message);
    }
}

