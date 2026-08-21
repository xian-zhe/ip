public class Deadlines extends Task{

    protected String deadline;
    public Deadlines(String description, String deadline) {
        super(description);
        this.deadline = deadline;
    }
    @Override
    public String toString() {
        return String.format("[D][%s] %s (by: %s)", super.getStatusIcon(), this.description, this.deadline);
    }
}