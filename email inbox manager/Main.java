import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

class Email {
    String id, sender, subject, body;
    int importance;          // 3 = High, 2 = Normal, 1 = Low
    long arrival;            // arrival time (millis)
    boolean read;

    Email(String id, String sender, String subject, String body, int importance, long arrival, boolean read) {
        this.id = id; this.sender = sender; this.subject = subject; this.body = body;
        this.importance = importance; this.arrival = arrival; this.read = read;
    }

    String level() { return importance == 3 ? "High" : importance == 2 ? "Normal" : "Low"; }

    String date() { return new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date(arrival)); }

    String toFileLine() {
        return String.join("|", id, sender, subject, body, "" + importance, "" + arrival, "" + read);
    }

    @Override
    public String toString() {
        return String.format("%-6s | %-14s | %-6s | %-22s | %s | %s",
                id, sender, level(), subject, date(), read ? "Read" : "Unread");
    }
}

public class Main {
    static final String FILE = "inbox.txt";

    // PRIORITY QUEUE: higher importance first; ties -> earlier arrival first
    static PriorityQueue<Email> inbox = new PriorityQueue<>((a, b) -> {
        if (a.importance != b.importance) return b.importance - a.importance;
        if (a.arrival != b.arrival) return Long.compare(a.arrival, b.arrival);
        return a.id.compareTo(b.id);
    });
    // HASHMAP: email-ID -> Email (O(1) lookup)
    static HashMap<String, Email> map = new HashMap<>();
    // STACK: recently opened emails (top = latest)
    static Stack<Email> recent = new Stack<>();

    static int nextNo = 101;
    static Scanner sc = new Scanner(System.in);

    // ---------- helpers ----------
    static String ask(String msg) {
        System.out.print(msg);
        return sc.hasNextLine() ? sc.nextLine().trim().replace("|", "/") : "";
    }

    static int parseImportance(String s) {
        s = s.toLowerCase();
        if (s.equals("high") || s.equals("h") || s.equals("3")) return 3;
        if (s.equals("normal") || s.equals("n") || s.equals("medium") || s.equals("2")) return 2;
        if (s.equals("low") || s.equals("l") || s.equals("1")) return 1;
        return 0;
    }

    static void add(Email e) {
        inbox.offer(e);
        map.put(e.id, e);
    }

    // ---------- file handling ----------
    static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|", -1);
                try {
                    Email e = new Email(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]),
                            Long.parseLong(p[5]), Boolean.parseBoolean(p[6]));
                    add(e);
                    int n = Integer.parseInt(p[0].substring(2));
                    if (n >= nextNo) nextNo = n + 1;
                } catch (Exception ex) {
                    System.out.println("Skipped invalid line in " + FILE);
                }
            }
            System.out.println(map.size() + " email(s) loaded from " + FILE);
        } catch (IOException ex) {
            System.out.println("Could not read " + FILE);
        }
    }

    static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Email e : map.values()) pw.println(e.toFileLine());
            System.out.println("Inbox saved to " + FILE);
        } catch (IOException ex) {
            System.out.println("Could not save " + FILE);
        }
    }

    // ---------- features ----------
    static void receive() {
        String sender = ask("Sender: ");
        String subject = ask("Subject: ");
        if (sender.isEmpty() || subject.isEmpty()) {
            System.out.println("Error: sender and subject are required.");
            return;
        }
        String body = ask("Message: ");
        int imp = parseImportance(ask("Importance (High/Normal/Low): "));
        if (imp == 0) {
            System.out.println("Error: importance must be High, Normal or Low.");
            return;
        }
        Email e = new Email("EM" + nextNo++, sender, subject, body, imp, System.currentTimeMillis(), false);
        add(e);
        System.out.println("Email received: " + e.id + " | " + e.sender + " | " + e.level());
    }

    static void open(Email e) {
        e.read = true;
        recent.remove(e);
        recent.push(e);
        System.out.println("Opened: " + e.id + " | " + e.sender);
        System.out.println("Subject : " + e.subject);
        System.out.println("Message : " + e.body);
        System.out.println("Priority: " + e.level());
    }

    static void openNext() {
        if (inbox.isEmpty()) { System.out.println("Inbox is empty."); return; }
        PriorityQueue<Email> copy = new PriorityQueue<>(inbox);
        while (!copy.isEmpty()) {
            Email e = copy.poll();
            if (!e.read) { open(e); return; }
        }
        System.out.println("No unread emails.");
    }

    static void openById() {
        String id = ask("Email ID: ").toUpperCase();
        Email e = map.get(id);
        if (e == null) System.out.println("Email " + id + " not found.");
        else open(e);
    }

    static void search() {
        String id = ask("Search Email ID: ").toUpperCase();
        Email e = map.get(id);
        if (e == null) System.out.println("Email " + id + " not found.");
        else System.out.println("Found: " + e);
    }

    static void markRead() {
        String id = ask("Email ID to mark as read: ").toUpperCase();
        Email e = map.get(id);
        if (e == null) System.out.println("Email " + id + " not found.");
        else { e.read = true; System.out.println(id + " marked as read."); }
    }

    static void showRecent() {
        if (recent.isEmpty()) { System.out.println("No recently opened emails."); return; }
        System.out.println("--- Recently opened (latest first) ---");
        for (int i = recent.size() - 1; i >= 0; i--) System.out.println(recent.get(i));
    }

    static void display() {
        if (inbox.isEmpty()) { System.out.println("Inbox is empty."); return; }
        PriorityQueue<Email> copy = new PriorityQueue<>(inbox);
        System.out.println("--- INBOX (priority order) ---");
        while (!copy.isEmpty()) System.out.println(copy.poll());
    }

    static void sortBy(boolean bySender) {
        if (map.isEmpty()) { System.out.println("Inbox is empty."); return; }
        List<Email> list = new ArrayList<>(map.values());
        if (bySender)
            list.sort((a, b) -> {
                int c = a.sender.compareToIgnoreCase(b.sender);
                return c != 0 ? c : Long.compare(a.arrival, b.arrival);
            });
        else
            list.sort((a, b) -> Long.compare(b.arrival, a.arrival));   // newest first
        System.out.println(bySender ? "--- Sorted by sender (A-Z) ---" : "--- Sorted by date (newest first) ---");
        for (Email e : list) System.out.println(e);
    }

    // ---------- main ----------
    public static void main(String[] args) {
        load();
        String choice;
        do {
            System.out.println("\n===== EMAIL INBOX MANAGER =====");
            System.out.println("1. Receive email");
            System.out.println("2. Open next email (highest priority)");
            System.out.println("3. Open email by ID");
            System.out.println("4. Search email by ID");
            System.out.println("5. Recently opened emails (stack)");
            System.out.println("6. Sort by sender");
            System.out.println("7. Sort by date");
            System.out.println("8. Mark email as read");
            System.out.println("9. Display inbox");
            System.out.println("0. Save and exit");
            choice = ask("Choice: ");
            switch (choice) {
                case "1": receive(); break;
                case "2": openNext(); break;
                case "3": openById(); break;
                case "4": search(); break;
                case "5": showRecent(); break;
                case "6": sortBy(true); break;
                case "7": sortBy(false); break;
                case "8": markRead(); break;
                case "9": display(); break;
                case "0": save(); System.out.println("Goodbye!"); break;
                default: System.out.println("Invalid choice.");
            }
        } while (!choice.equals("0"));
    }
}

// ===== EMAIL INBOX MANAGER =====
// 1. Receive email
// 2. Open next email (highest priority)
// 3. Open email by ID
// 4. Search email by ID
// 5. Recently opened emails (stack)
// 6. Sort by sender
// 7. Sort by date
// 8. Mark email as read
// 9. Display inbox
// 0. Save and exit
// Sender: 1
// Subject: 1
// Message: 1
// Importance (High/Normal/Low): 1
// Email received: EM101 | 1 | Low