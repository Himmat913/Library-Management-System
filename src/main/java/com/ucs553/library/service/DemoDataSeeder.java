package com.ucs553.library.service;

import com.ucs553.library.dao.Repositories;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.fine.FinePolicy;
import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;
import com.ucs553.library.model.Book;
import com.ucs553.library.model.DigitalResource;
import com.ucs553.library.model.Fine;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.ItemType;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.Magazine;
import com.ucs553.library.model.Member;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

public class DemoDataSeeder {

    public static final String DEMO_PASSWORD = "member123";

    private static final Logger LOG = Logger.getLogger(DemoDataSeeder.class.getName());

    // title | author | category | year
    private static final String[] BOOKS = {
            "Java: The Complete Reference|Herbert Schildt|Programming|2022",
            "Effective Java|Joshua Bloch|Programming|2018",
            "Head First Java|Kathy Sierra|Programming|2022",
            "Clean Code|Robert C. Martin|Programming|2008",
            "Clean Architecture|Robert C. Martin|Programming|2017",
            "Design Patterns|Erich Gamma|Programming|1994",
            "Refactoring|Martin Fowler|Programming|2018",
            "The Pragmatic Programmer|Andrew Hunt|Programming|2019",
            "Code Complete|Steve McConnell|Programming|2004",
            "Java Concurrency in Practice|Brian Goetz|Programming|2006",
            "Head First Design Patterns|Eric Freeman|Programming|2020",
            "Python Crash Course|Eric Matthes|Programming|2023",
            "Fluent Python|Luciano Ramalho|Programming|2022",
            "JavaScript: The Good Parts|Douglas Crockford|Programming|2008",
            "The C Programming Language|Brian Kernighan|Programming|1988",
            "Learning SQL|Alan Beaulieu|Programming|2020",
            "Database System Concepts|Abraham Silberschatz|Computer Science|2019",
            "Introduction to Algorithms|Thomas H. Cormen|Computer Science|2022",
            "Algorithms|Robert Sedgewick|Computer Science|2011",
            "Operating System Concepts|Abraham Silberschatz|Computer Science|2018",
            "Computer Networks|Andrew Tanenbaum|Computer Science|2021",
            "Artificial Intelligence: A Modern Approach|Stuart Russell|Computer Science|2020",
            "Computer Organization and Design|David Patterson|Computer Science|2020",
            "Compilers: Principles, Techniques and Tools|Alfred Aho|Computer Science|2006",
            "Discrete Mathematics and Its Applications|Kenneth Rosen|Computer Science|2018",
            "Structure and Interpretation of Computer Programs|Harold Abelson|Computer Science|1996",
            "The Art of Computer Programming|Donald Knuth|Computer Science|2011",
            "Deep Learning|Ian Goodfellow|Computer Science|2016",
            "Hands-On Machine Learning|Aurelien Geron|Computer Science|2022",
            "Software Engineering|Ian Sommerville|Computer Science|2015",
            "Introduction to the Theory of Computation|Michael Sipser|Computer Science|2012",
            "To Kill a Mockingbird|Harper Lee|Fiction|1960",
            "1984|George Orwell|Fiction|1949",
            "Pride and Prejudice|Jane Austen|Fiction|1813",
            "The Great Gatsby|F. Scott Fitzgerald|Fiction|1925",
            "The Catcher in the Rye|J. D. Salinger|Fiction|1951",
            "Animal Farm|George Orwell|Fiction|1945",
            "The Alchemist|Paulo Coelho|Fiction|1988",
            "Brave New World|Aldous Huxley|Fiction|1932",
            "The Kite Runner|Khaled Hosseini|Fiction|2003",
            "Midnight's Children|Salman Rushdie|Fiction|1981",
            "The God of Small Things|Arundhati Roy|Fiction|1997",
            "The White Tiger|Aravind Adiga|Fiction|2008",
            "Godan|Munshi Premchand|Fiction|1936",
            "Malgudi Days|R. K. Narayan|Fiction|1943",
            "The Guide|R. K. Narayan|Fiction|1958",
            "Gitanjali|Rabindranath Tagore|Fiction|1910",
            "A Brief History of Time|Stephen Hawking|Science|1988",
            "Cosmos|Carl Sagan|Science|1980",
            "The Selfish Gene|Richard Dawkins|Science|1976",
            "Sapiens|Yuval Noah Harari|Science|2011",
            "The Origin of Species|Charles Darwin|Science|1859",
            "Surely You're Joking, Mr. Feynman|Richard Feynman|Science|1985",
            "Astrophysics for People in a Hurry|Neil deGrasse Tyson|Science|2017",
            "The Elegant Universe|Brian Greene|Science|1999",
            "Wings of Fire|A. P. J. Abdul Kalam|Science|1999",
            "The Discovery of India|Jawaharlal Nehru|History|1946",
            "India After Gandhi|Ramachandra Guha|History|2007",
            "My Experiments with Truth|M. K. Gandhi|History|1927",
            "Guns, Germs, and Steel|Jared Diamond|History|1997",
            "The Diary of a Young Girl|Anne Frank|History|1947",
            "Long Walk to Freedom|Nelson Mandela|History|1994",
            "Atomic Habits|James Clear|Self-help|2018",
            "Rich Dad Poor Dad|Robert Kiyosaki|Self-help|1997",
            "Deep Work|Cal Newport|Self-help|2016",
            "Thinking, Fast and Slow|Daniel Kahneman|Self-help|2011",
            "The Lean Startup|Eric Ries|Self-help|2011",
            "Zero to One|Peter Thiel|Self-help|2014",
            "The 7 Habits of Highly Effective People|Stephen Covey|Self-help|1989",
            "Ikigai|Hector Garcia|Self-help|2016",
            "Think and Grow Rich|Napoleon Hill|Self-help|1937",
            "Higher Engineering Mathematics|B. S. Grewal|Engineering|2017",
            "Concepts of Physics|H. C. Verma|Engineering|1992",
            "Fundamentals of Physics|David Halliday|Engineering|2018",
            "Thermodynamics: An Engineering Approach|Yunus Cengel|Engineering|2018",
            "Electronic Devices and Circuit Theory|Robert Boylestad|Engineering|2012",
            "Digital Design|M. Morris Mano|Engineering|2018",
            "Signals and Systems|Alan Oppenheim|Engineering|1996",
            "Control Systems Engineering|Norman Nise|Engineering|2019"
    };

    // title | publisher
    private static final String[] MAGAZINES = {
            "National Geographic|NatGeo Partners", "IEEE Spectrum|IEEE", "Wired|Conde Nast",
            "The Economist|The Economist Group", "Scientific American|Springer Nature", "Nature India|Springer Nature",
            "Outlook|Outlook Publishing", "India Today|Living Media", "Frontline|The Hindu Group",
            "Reader's Digest|Trusted Media Brands", "Computer|IEEE Computer Society",
            "Communications of the ACM|ACM", "Digit|9.9 Group", "Down To Earth|Society for Environmental Communications"
    };

    // title | author | format | size in MB
    private static final String[] DIGITAL = {
            "Think Java|Allen Downey|pdf|3.8", "The Java Language Specification|Oracle|pdf|4.2",
            "The Java Tutorials|Oracle|html|12.0", "JavaFX Documentation|OpenJFX|html|8.5",
            "Eloquent JavaScript|Marijn Haverbeke|pdf|5.1", "Think Python|Allen Downey|pdf|2.9",
            "Pro Git|Scott Chacon|epub|6.3", "The Rust Programming Language|Steve Klabnik|epub|4.4",
            "SQLite Documentation|SQLite Consortium|html|9.7", "Maven: The Complete Reference|Sonatype|pdf|7.2",
            "The Linux Command Line|William Shotts|pdf|3.1", "Operating Systems: Three Easy Pieces|Remzi Arpaci-Dusseau|pdf|11.5",
            "Mathematics for Machine Learning|Marc Deisenroth|pdf|9.8", "Open Data Structures|Pat Morin|pdf|2.6",
            "Automate the Boring Stuff|Al Sweigart|epub|5.9", "Dive into Deep Learning|Aston Zhang|pdf|24.0"
    };

    private static final String[] FIRST_NAMES = {
            "Aarav", "Priya", "Rahul", "Simran", "Ananya", "Vikram", "Neha", "Arjun", "Kavya", "Rohan", "Isha",
            "Karan", "Meera", "Aditya", "Sanya", "Harsh", "Tanvi", "Manav", "Riya", "Dev", "Pooja", "Yash", "Nisha", "Ishaan"
    };
    private static final String[] LAST_NAMES = {
            "Mehta", "Sharma", "Verma", "Kaur", "Singh", "Gupta", "Kapoor", "Malhotra", "Bansal", "Chopra", "Arora", "Bhatia"
    };

    private static final int MEMBER_COUNT = 60;

    private final Repositories repos;
    private final LibraryService service;
    private final AuthService auth;
    private final FinePolicy finePolicy;
    private final Clock clock;
    private final Random random = new Random(42);

    public DemoDataSeeder(Repositories repos, LibraryService service, AuthService auth, FinePolicy finePolicy, Clock clock) {
        this.repos = repos;
        this.service = service;
        this.auth = auth;
        this.finePolicy = finePolicy;
        this.clock = clock;
    }

    public boolean seed() throws LibraryException {
        if (!repos.items().findAll().isEmpty() || !repos.members().findAll().isEmpty()) {
            LOG.info("Database already has data, demo data not added");
            return false;
        }
        LocalDate today = LocalDate.now(clock);

        List<Item> catalogue = buildCatalogue();
        for (Item item : catalogue) {
            repos.items().insert(item);
        }
        List<Member> people = buildMembers(today);
        for (Member member : people) {
            repos.members().insert(member);
        }
        createAccounts(people);

        Map<Integer, Integer> activeByMember = new HashMap<>();
        Set<String> pairs = new HashSet<>();
        addHistory(catalogue, people, today);
        Map<Item, Loan> scarceLoans = addScarceLoans(catalogue, people, today, activeByMember, pairs);
        addActiveLoans(catalogue, people, today, activeByMember, pairs);
        addReservations(scarceLoans, people);

        LOG.info("Demo data added: " + catalogue.size() + " items, " + people.size() + " members");
        return true;
    }

    private List<Item> buildCatalogue() {
        List<Item> catalogue = new ArrayList<>();
        int number = 1;
        for (String line : BOOKS) {
            String[] f = line.split("\\|");
            Item book = new Book.Builder().title(f[0]).author(f[1]).year(Integer.parseInt(f[3]))
                    .copies(1 + random.nextInt(3)).isbn(String.format("DEMO-%04d", number++))
                    .pages(180 + random.nextInt(900)).build();
            book.setCategory(f[2]);
            catalogue.add(book);
        }
        int issue = 1;
        for (String line : MAGAZINES) {
            String[] f = line.split("\\|");
            Item magazine = new Magazine(f[0], "Various", 2026, 1 + random.nextInt(3), issue++, f[1]);
            magazine.setCategory("Magazines");
            catalogue.add(magazine);
        }
        for (String line : DIGITAL) {
            String[] f = line.split("\\|");
            Item digital = new DigitalResource(f[0], f[1], 2020 + random.nextInt(6), 5 + random.nextInt(6),
                    f[2], Double.parseDouble(f[3]));
            digital.setCategory("Digital library");
            catalogue.add(digital);
        }
        return catalogue;
    }

    private List<Member> buildMembers(LocalDate today) {
        List<Member> people = new ArrayList<>();
        Set<String> used = new HashSet<>();
        String[][] fixed = {{"Aarav", "Mehta"}, {"Priya", "Sharma"}, {"Rahul", "Verma"}, {"Simran", "Kaur"}};
        for (String[] name : fixed) {
            used.add(name[0] + " " + name[1]);
            int limit = name[0].equals("Simran") ? 2 : Member.DEFAULT_BORROW_LIMIT;
            people.add(member(name[0], name[1], today.minusMonths(6), limit));
        }
        while (people.size() < MEMBER_COUNT) {
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            if (used.add(first + " " + last)) {
                int limit = random.nextInt(8) == 0 ? 3 : Member.DEFAULT_BORROW_LIMIT;
                people.add(member(first, last, today.minusDays(30 + random.nextInt(700)), limit));
            }
        }
        return people;
    }

    private Member member(String first, String last, LocalDate joined, int limit) {
        return new Member(first + " " + last, first.toLowerCase() + "." + last.toLowerCase() + "@example.com", joined, limit);
    }

    private void createAccounts(List<Member> people) throws LibraryException {
        String hash = PasswordHasher.hash(DEMO_PASSWORD.toCharArray());
        for (Member member : people) {
            auth.setMemberPassword(member.getId(), hash, false);
        }
    }

    // returned loans from the past months, some late with a fine that is mostly paid
    private void addHistory(List<Item> catalogue, List<Member> people, LocalDate today) throws LibraryException {
        for (int i = 0; i < 110; i++) {
            Item item = catalogue.get(random.nextInt(catalogue.size()));
            Member member = people.get(random.nextInt(people.size()));
            LocalDate issued = today.minusDays(25 + random.nextInt(130));
            LocalDate due = issued.plusDays(item.getType().getLoanDays());
            int late = random.nextInt(4) == 0 ? 1 + random.nextInt(10) : 0;
            LocalDate returned = late > 0 ? due.plusDays(late) : issued.plusDays(1 + random.nextInt(item.getType().getLoanDays()));

            Loan loan = new Loan(0, item.getId(), member.getId(), issued, due, returned);
            repos.loans().insert(loan);
            logEvent(issued, AuditAction.ISSUED, item.getTitle() + " to " + member.getName() + ", due " + due);
            logEvent(returned, AuditAction.RETURNED, item.getTitle() + " from " + member.getName());

            BigDecimal amount = finePolicy.calculate(item.getType(), loan.overdueDays(today));
            if (amount.signum() > 0) {
                Fine fine = new Fine(loan.getId(), member.getId(), amount, returned);
                if (random.nextInt(10) < 7) {
                    fine.markPaid(returned.plusDays(random.nextInt(3)));
                }
                repos.fines().insert(fine);
                logEvent(returned, AuditAction.FINED, member.getName() + " fined ₹" + amount + " for "
                        + late + " days late on " + item.getTitle());
            }
        }
    }

    // single-copy books that are all out, so there is something to reserve
    private Map<Item, Loan> addScarceLoans(List<Item> catalogue, List<Member> people, LocalDate today,
                                           Map<Integer, Integer> activeByMember, Set<String> pairs) throws LibraryException {
        Map<Item, Loan> scarce = new HashMap<>();
        for (Item item : catalogue) {
            if (scarce.size() >= 8) {
                break;
            }
            if (item.getType() == ItemType.BOOK && item.getTotalCopies() == 1) {
                Member holder = people.get(random.nextInt(people.size()));
                if (canLend(holder, item, activeByMember, pairs)) {
                    scarce.put(item, lend(item, holder, today.minusDays(1 + random.nextInt(10)), activeByMember, pairs));
                }
            }
        }
        return scarce;
    }

    private void addActiveLoans(List<Item> catalogue, List<Member> people, LocalDate today,
                                Map<Integer, Integer> activeByMember, Set<String> pairs) throws LibraryException {
        int made = 0;
        int attempts = 0;
        while (made < 45 && attempts++ < 600) {
            Item item = catalogue.get(random.nextInt(catalogue.size()));
            Member member = people.get(random.nextInt(people.size()));
            if (item.isAvailable() && canLend(member, item, activeByMember, pairs)) {
                lend(item, member, today.minusDays(random.nextInt(30)), activeByMember, pairs);
                made++;
            }
        }
    }

    private void addReservations(Map<Item, Loan> scarceLoans, List<Member> people) throws LibraryException {
        List<Item> held = new ArrayList<>(scarceLoans.keySet());
        held.sort(null);
        for (Item item : held) {
            int wanted = 1 + random.nextInt(3);
            int tries = 0;
            while (wanted > 0 && tries++ < 20) {
                Member member = people.get(random.nextInt(people.size()));
                try {
                    service.reserve(item.getId(), member.getId());
                    wanted--;
                } catch (LibraryException skipped) {
                    // that member already holds or waits for this item, try someone else
                }
            }
        }
        if (!held.isEmpty()) {
            service.returnLoan(scarceLoans.get(held.get(0)).getId());
        }
    }

    private boolean canLend(Member member, Item item, Map<Integer, Integer> activeByMember, Set<String> pairs) {
        int active = activeByMember.getOrDefault(member.getId(), 0);
        return active < Math.min(member.getBorrowLimit(), 3) && !pairs.contains(member.getId() + ":" + item.getId());
    }

    private Loan lend(Item item, Member member, LocalDate issuedOn, Map<Integer, Integer> activeByMember,
                      Set<String> pairs) throws LibraryException {
        item.checkOut();
        repos.items().update(item);
        Loan loan = new Loan(item.getId(), member.getId(), issuedOn, item.getType());
        repos.loans().insert(loan);
        activeByMember.merge(member.getId(), 1, Integer::sum);
        pairs.add(member.getId() + ":" + item.getId());
        logEvent(issuedOn, AuditAction.ISSUED, item.getTitle() + " to " + member.getName() + ", due " + loan.getDueOn());
        return loan;
    }

    private void logEvent(LocalDate day, AuditAction action, String details) throws LibraryException {
        repos.audit().insert(new AuditEntry(day.atTime(9 + random.nextInt(8), random.nextInt(60)), "demo", action, details));
    }
}
