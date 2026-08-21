import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
    //regex patterns
    private static final Pattern MARK_PATTERN =
            Pattern.compile("^mark\\s+(?<index>\\d+)$");
    private static final Pattern UNMARK_PATTERN =
            Pattern.compile("^unmark\\s+(?<index>\\d+)$");
    public static void main(String[] args) {
        String banner = "  ___    ____ \n"
                + " / _ \\  |_  /\n"
                + "| | | |   / / \n"
                + "| |_| |  / /_ \n"
                + " \\___/  /____|\n";
        String greeting = DIVIDER +
                banner +
                "Hello! I'm Oz.\n" +
                "What can I do for you? ᕙ(  •̀ ᗜ •́  )ᕗ\n";

        String bye = DIVIDER +
                "  Bye. Hope to see you again soon! („• ֊ •„)੭\n" +
                DIVIDER;

        System.out.println(greeting);

        Scanner scanner = new Scanner(System.in);


        String desc = "";

        Task[] list = new Task[100];
        int counter = 0;

        while(true) {
            desc = scanner.nextLine();
            //set up matchers
            Matcher markMatcher = MARK_PATTERN.matcher(desc);
            Matcher unmarkMatcher = UNMARK_PATTERN.matcher(desc);
            String reply = "";
            if (desc.equals("bye")) {
                break;
            } else if(desc.equals("list")) {
                reply += DIVIDER + "Here are the tasks in your list:\n";
                for(int i = 0; i < counter; i++){
                    reply += String.format("%d. %s%n",i + 1, list[i]);
                }
                reply += DIVIDER;
            } else if (markMatcher.matches()) {

                int index = Integer.parseInt(markMatcher.group("index")) - 1;
                list[index].mark();
                reply += DIVIDER + "Nice! I've marked this task as done:\n  " + list[index] + "\n" + DIVIDER;

            } else if (unmarkMatcher.matches()) {

                int index = Integer.parseInt(unmarkMatcher.group("index")) - 1;
                list[index].unmark();
                reply += DIVIDER + "OK, I've marked this task as not done yet:\n  " + list[index] + "\n" + DIVIDER;

            }else {
                reply += DIVIDER + "added: " + desc + "\n" + DIVIDER;
                list[counter] = new Task(desc);
                counter++;
            }

            System.out.println(reply);

        }
        System.out.println(bye);
    }
}