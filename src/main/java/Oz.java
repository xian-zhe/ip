import java.util.Scanner;

public class Oz {
    private static final String DIVIDER = "____________________________________________________________\n";
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
                "Bye. Hope to see you again soon! („• ֊ •„)੭\n" +
                DIVIDER;

        System.out.println(greeting);

        Scanner scanner = new Scanner(System.in);
        String desc = "";

        String[] list = new String[100];
        int counter = 0;

        while(true) {
            desc = scanner.nextLine();
            if (desc.equals("bye")) {
                break;
            } else if(desc.equals("list")) {
                System.out.print(DIVIDER);
                for(int i = 0; i < counter; i++){
                    System.out.printf("%d. %s%n",i + 1, list[i]);
                }
                System.out.print(DIVIDER);
            } else {
                String reply = DIVIDER + "added: " + desc + "\n" + DIVIDER;
                System.out.println(reply);
                list[counter] = desc;
                counter++;
            }

        }
        System.out.println(bye);
    }
}