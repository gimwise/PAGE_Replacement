class Page {
    public static int pageCnt = 0;

    public enum STATUS {
        HIT, PAGEFAULT, MIGRATION
    }

    public int pid;
    public char data;
    public STATUS status;
}