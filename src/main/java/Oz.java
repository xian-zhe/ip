import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("^(?<command>\\S+)(?:\\s+(?<details>.*))?$");
    private static final Pattern DEADLINE_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/by\\s+(?<by>.+)$");
    private static final Pattern EVENT_ARGUMENTS_PATTERN =
            Pattern.compile("^(?<desc>.+?)\\s+/from\\s+(?<from>.+?)\\s+/to\\s+(?<to>.+)$");

    public static void main(String[] args) {
        String banner = """
                  ___    ____\s
                 / _ \\  |_  /
                | | | |   / /\s
                | |_| |  / /_\s
                 \\___/  /____|
                """;
        String greeting = DIVIDER +
                banner +
                "Hello! I'm Oz.\n" +
                "What can I do for you? ᕙ(  •̀ ᗜ •́  )ᕗ\n";

        String bye = DIVIDER +
                "  Bye. Hope to see you again soon! („• ֊ •„)੭\n" +
                DIVIDER;

        System.out.print(greeting);

        Scanner scanner = new Scanner(System.in);

        ArrayList<Task> list = new ArrayList<>();

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

                } else if (command.equals("mark")) {
                    int index = parseTaskIndex(details, list.size());
                    list.get(index).mark();
                    reply = DIVIDER
                            + "Nice! I've marked this task as done:\n  "
                            + list.get(index) + "\n" + DIVIDER;

                } else if (command.equals("unmark")) {
                    int index = parseTaskIndex(details, list.size());
                    list.get(index).unmark();
                    reply = DIVIDER
                            + "OK! I've marked this task as not done yet:\n  "
                            + list.get(index) + "\n" + DIVIDER;

                } else if (command.equals("todo")) {
                    if (details.isBlank()) {
                        throw new OzException(
                                "The description of a todo cannot be empty.");
                    }

                    list.add(new ToDo(details));
                    reply = DIVIDER
                            + String.format(
                            """
                                    Got it. I've added this task:
                                    %s
                                    Now you have %d tasks in the list.
                                    """,
                                    list.getLast(), list.size())
                            + DIVIDER;

                } else if (command.equals("deadline")) {
                    Matcher deadlineMatcher =
                            DEADLINE_ARGUMENTS_PATTERN.matcher(details);
                    if (!deadlineMatcher.matches()) {
                        throw new OzException(
                                "Use: deadline <description> /by <date>.");
                    }

                    list.add(new Deadlines(
                            deadlineMatcher.group("desc").trim(),
                            deadlineMatcher.group("by").trim()));
                    reply = DIVIDER
                            + String.format(
                            """
                                    Got it. I've added this task:
                                    %s
                                    Now you have %d tasks in the list.
                                    """,
                            list.getLast(), list.size())
                            + DIVIDER;

                } else if (command.equals("event")) {
                    Matcher eventMatcher =
                            EVENT_ARGUMENTS_PATTERN.matcher(details);
                    if (!eventMatcher.matches()) {
                        throw new OzException(
                                "Use: event <description> /from <start> /to <end>.");
                    }

                    list.add(new Event(
                            eventMatcher.group("desc").trim(),
                            eventMatcher.group("from").trim(),
                            eventMatcher.group("to").trim()));
                    reply = DIVIDER
                            + String.format(
                            """
                                    Got it. I've added this task:
                                    %s
                                    Now you have %d tasks in the list.
                                    """,
                            list.getLast(), list.size())
                            + DIVIDER;

                } else if (command.equals("delete")) {
                    int index = parseTaskIndex(details, list.size());
                    Task t = list.get(index);
                    list.remove(index);
                    reply = DIVIDER
                            + String.format(
                            """
                                    Ok the following task has been removed!:
                                    %s
                                    Now you have %d tasks in the list.
                                    """,
                            t, list.size())
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

        System.out.print(bye);
    }

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
}
