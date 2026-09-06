package oz;

/**
 * Represents the type or category of a user command executed by Oz.
 */
public enum CommandType {
    /** Adding a new task (todo, deadline, event). */
    ADD,

    /** Marking or unmarking a task. */
    CHANGE_MARK,

    /** Deleting an existing task. */
    DELETE,

    /** Listing tasks. */
    LIST,

    /** Searching for tasks by keyword. */
    FIND,

    /** Exiting the application. */
    BYE,

    /** Empty or blank input. */
    BLANK,

    /** Execution or parsing error. */
    ERROR,

    /** Default or unclassified command. */
    DEFAULT
}
