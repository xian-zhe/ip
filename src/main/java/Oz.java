public class Oz {
    public static void main(String[] args) {
        String banner = "  ___    ____ \n"
                + " / _ \\  |_  /\n"
                + "| | | |   / / \n"
                + "| |_| |  / /_ \n"
                + " \\___/  /____|\n";
        String greeting = "____________________________________________________________\n" +
                banner +
                "Hello! I'm Oz.\n" +
                "What can I do for you?\n";

        String bye = "____________________________________________________________\n" +
                "Bye. Hope to see you again soon!\n" +
                "____________________________________________________________\n";

        System.out.println(greeting);
        System.out.println(bye);
    }
}