import java.util.Scanner;

public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
    public static void main(String[] args) {
        String banner = "  ___    ____ \n"
                + " / _ \\  |_  /\n"
                + "| | | |   / / \n"
                + "| |_| |  / /_ \n"
                + " \\___/  /____|\n";
        String greeting = "____________________________________________________________\n" +
                banner +
                "Hello! I'm Oz.\n" +
                "What can I do for you? ᕙ(  •̀ ᗜ •́  )ᕗ\n";

        String bye = "____________________________________________________________\n" +
                "Bye. Hope to see you again soon! („• ֊ •„)੭\n" +
                "____________________________________________________________\n";

        System.out.println(greeting);

        Scanner scanner = new Scanner(System.in);
        String desc = "";

        while (true) {
            desc = scanner.nextLine();
            if (desc.equals("bye")){
                break;
            }
            String reply = DIVIDER + desc + "\n" + DIVIDER;
            System.out.println(reply);
        }
        System.out.println(bye);
    }
}