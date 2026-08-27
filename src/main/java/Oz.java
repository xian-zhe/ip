import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point and controller for the Oz chatbot.
 */
public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("^(?<command>\\S+)(?:\\s+(?<details>.*))?$");
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/by\\s+(?<by>.+)$");
    private static final Pattern EVENT_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/from\\s+(?<from>.+?)\\s+/to\\s+(?<to>.+)$");

    private final Storage storage;

    /**
     * Constructs an Oz chatbot instance configured with the specified storage file path.
     *
     * @param filePath Path to the task storage file.
     */
    public Oz(String filePath) {
        this.storage = new Storage(filePath);
    }

    /**
     * Runs the main command loop of the chatbot.
     */
    public void run() {
        String banner = """
                  ___    ____\s
                 / _ \\  |_  /
                | | | |   / /\s
                | |_| |  / /_\s
                 \\___/  /____|
                """;
        String greeting = DIVIDER
                + banner
                + "Hello! I'm Oz.\n"
                + "What can I do for you? ᕙ(  •̀ ᗜ •́  )ᕗ\n";

        String bye = DIVIDER
                + "  Bye. Hope to see you again soon! („• ֊ •„)੭\n"
                + DIVIDER;

        System.out.print(greeting);

        try (Scanner scanner = new Scanner(System.in)) {
            ArrayList<Task> list = this.storage.load();

            while (scanner.hasNextLine()) {
                String desc = scanner.nextLine().trim();
                if (desc.equals("bye")) {
                    break;
                }

                String reply;
                try {
                    Matcher commandMatcher = COMMAND_PATTERN.matcher(desc);
                    if (!commandMatcher.matches()) {
                        throw new OzException("I could not understand that input.");
                    }

                    String command = commandMatcher.group("command");
                    String details = commandMatcher.group("details");
                    if (details == null) {
                        details = "";
                    }
                    details = details.trim();

                    if (command.equals("list")) {
                        if (!details.isBlank()) {
                            throw new OzException("The list command does not take arguments.");
                        }

                        StringBuilder response =
                                new StringBuilder(DIVIDER + "Here are the tasks in your list:\n");
                        for (int i = 0; i < list.size(); i++) {
                            response.append(i + 1)
                                    .append(". ")
                                    .append(list.get(i))
                                    .append("\n");
                        }
                        reply = response.append(DIVIDER).toString();

                    } else if (command.equals("on")) {
                        if (details.isBlank()) {
                            throw new OzException("Use: on <date> (e.g., on 2019-10-15 or on 2/12/2019).");
                        }

                        TaskDateTime targetDateTime = TaskDateTime.parse(details);
                        LocalDate targetDate = targetDateTime.toLocalDate();
                        String dateHeader = targetDate.format(
                                DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH));

                        ArrayList<Task> matchingTasks = new ArrayList<>();
                        for (Task task : list) {
                            if (task.occursOn(targetDate)) {
                                matchingTasks.add(task);
                            }
                        }

                        if (matchingTasks.isEmpty()) {
                            reply = DIVIDER
                                    + "There are no tasks occurring on " + dateHeader + ".\n"
                                    + DIVIDER;
                        } else {
                            StringBuilder response = new StringBuilder(DIVIDER
                                    + "Here are the tasks occurring on " + dateHeader + ":\n");
                            for (int i = 0; i < matchingTasks.size(); i++) {
                                response.append(i + 1)
                                        .append(". ")
                                        .append(matchingTasks.get(i))
                                        .append("\n");
                            }
                            reply = response.append(DIVIDER).toString();
                        }

                    } else if (command.equals("mark")) {
                        int index = parseTaskIndex(details, list.size());
                        list.get(index).mark();
                        this.storage.save(list);
                        reply = DIVIDER
                                + "Nice! I've marked this task as done:\n  "
                                + list.get(index) + "\n" + DIVIDER;

                    } else if (command.equals("unmark")) {
                        int index = parseTaskIndex(details, list.size());
                        list.get(index).unmark();
                        this.storage.save(list);
                        reply = DIVIDER
                                + "OK! I've marked this task as not done yet:\n  "
                                + list.get(index) + "\n" + DIVIDER;

                    } else if (command.equals("todo")) {
                        if (details.isBlank()) {
                            throw new OzException(
                                    "The description of a todo cannot be empty.");
                        }

                        list.add(new ToDo(details));
                        this.storage.save(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("deadline")) {
                        Matcher deadlineMatcher =
                                DEADLINE_ARGUMENTS_PATTERN.matcher(details);
                        if (!deadlineMatcher.matches()) {
                            throw new OzException(
                                    "Use: deadline <description> /by <date>.");
                        }

                        String deadlineDesc = deadlineMatcher.group("desc").trim();
                        String by = deadlineMatcher.group("by").trim();
                        if (deadlineDesc.isEmpty()) {
                            throw new OzException("The description of a deadline cannot be empty.");
                        }
                        if (by.isEmpty()) {
                            throw new OzException("The deadline date/time (/by) cannot be empty.");
                        }

                        TaskDateTime deadlineTime = TaskDateTime.parse(by);
                        list.add(new Deadlines(deadlineDesc, deadlineTime));
                        this.storage.save(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("event")) {
                        Matcher eventMatcher =
                                EVENT_ARGUMENTS_PATTERN.matcher(details);
                        if (!eventMatcher.matches()) {
                            throw new OzException(
                                    "Use: event <description> /from <start> /to <end>.");
                        }

                        String eventDesc = eventMatcher.group("desc").trim();
                        String from = eventMatcher.group("from").trim();
                        String to = eventMatcher.group("to").trim();
                        if (eventDesc.isEmpty()) {
                            throw new OzException("The description of an event cannot be empty.");
                        }
                        if (from.isEmpty() || to.isEmpty()) {
                            throw new OzException("The event start (/from) and end (/to) dates cannot be empty.");
                        }

                        TaskDateTime fromTime = TaskDateTime.parse(from);
                        TaskDateTime toTime = TaskDateTime.parse(to);
                        list.add(new Event(eventDesc, fromTime, toTime));
                        this.storage.save(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Got it. I've added this task:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        list.get(list.size() - 1), list.size())
                                + DIVIDER;

                    } else if (command.equals("delete")) {
                        int index = parseTaskIndex(details, list.size());
                        Task removedTask = list.get(index);
                        list.remove(index);
                        this.storage.save(list);
                        reply = DIVIDER
                                + String.format(
                                """
                                        Ok the following task has been removed!:
                                        %s
                                        Now you have %d tasks in the list.
                                        """,
                                        removedTask, list.size())
                                + DIVIDER;

                    } else {
                        throw new OzException(
                                "Sorry, I do not understand that command.");
                    }
                } catch (OzException exception) {
                    reply = DIVIDER + "OOPS! " + exception.getMessage()
                            + "\n" + DIVIDER;
                }

                System.out.print(reply);
            }
        }

        System.out.print(bye);
    }

    /**
     * Parses the zero-based task index from user command arguments.
     *
     * @param args Arguments string containing the 1-based task number.
     * @param taskCount Current total number of tasks in the list.
     * @return 0-based task index.
     * @throws OzException If the input is invalid or out of range.
     */
    private static int parseTaskIndex(String args, int taskCount)
            throws OzException {
        if (!args.matches("\\d+")) {
            throw new OzException("Please provide a valid task number.");
        }

        try {
            int taskNumber = Integer.parseInt(args);
            if (taskNumber < 1 || taskNumber > taskCount) {
                throw new OzException("That task number does not exist.");
            }
            return taskNumber - 1;
        } catch (NumberFormatException exception) {
            throw new OzException("That task number is too large.");
        }
    }

    public static void main(String[] args) {
        new Oz("data/oz.txt").run();
    }
}

