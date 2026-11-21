# Library Management System

A desktop library management application written in Java for the UCS553 (Enterprise Web Application) course. It has a JavaFX interface, a SQLite database accessed through JDBC with the DAO pattern, sign-in for staff and for members, a reservation queue with stored fines, an activity log, and CSV import and export.

The project is also a demonstration of the course concepts: object-oriented design, generics, collections, exceptions, file handling, serialization, logging, JDBC, transactions and design patterns.

## Features

**For staff (librarians and administrators)**

- **Dashboard**: counts of items, members, loans, overdue loans, open reservations and unpaid fines. Each box opens the matching page. Also the most borrowed items and the latest activity.
- **Circulation**: issue, renew and return items, borrow limits, due dates, overdue highlighting, per-day fines by item type, and filters for active, overdue, returned and loans with fines.
- **Items**: books, magazines and digital resources with a category, search, a category filter, sorting, and a button to reserve an item for a member when every copy is out.
- **Members**: member list with search and a detail panel showing each member's active loans.
- **Reservations**: the waiting queue for every item. When a copy comes back it is held for the first person in line for a few days. Staff can hand it over, cancel a reservation, or release holds that were not collected in time.
- **Fines**: fines are stored when an item comes back late. Staff collect them here. A member whose unpaid fines reach a limit cannot borrow until they pay.
- **Activity**: an audit log of who issued, returned, renewed, fined, reserved or changed an account, and when, with search, filter and CSV export.
- **Reports**: overdue, most borrowed and outstanding fines reports, CSV export of items, members and loans, CSV import with row-by-row error reporting, and a database backup.
- **Logins (administrators)**: create or reset a member's login. A temporary password is shown once and has to be changed at the member's first sign in.
- **Staff accounts (administrators)**: sign-in with salted, hashed passwords. Administrators manage accounts and can delete records, import data and take backups. Librarians handle circulation and day-to-day edits.

**For members**

- Sign in with the email they registered with.
- Search the catalogue and filter by category. Reserve an item when every copy is out.
- See their own loans and renew them (once, and only when nobody is waiting and the loan is not overdue).
- See and cancel their own reservations, and see their fines.
- Read notifications, for example when a reserved item is ready. The sidebar shows the unread count.
- Change their password.

**Everyone**

- Light and dark theme. The choice is remembered, along with the window size, last page and last username.

## Requirements

- JDK 25
- Maven 3.9 or newer

JavaFX and the other libraries are downloaded by Maven.

## Build and run

```
mvn clean test
mvn javafx:run
```

On the first run an administrator account is created: username `admin`, password `admin123`. Change the password after signing in (**Change password** in the sidebar).

### Demo data

To start with a populated library:

```
mvn javafx:run "-Djavafx.args=--reset-db --seed-demo"
```

This adds 110 items (books in several categories, magazines and digital resources), 60 members, a history of returned loans with fines, current loans, and a few reservation queues. Every demo member has a login: the email is `first.last@example.com` (for example `aarav.mehta@example.com`) and the password is `member123`. Pick the **Member** tab on the sign-in screen.

### Command-line options

| Option | Meaning |
|---|---|
| `--config <file>` | Use a different properties file (default `config/app.properties`) |
| `--reset-db` | Drop and recreate all tables, including staff accounts |
| `--seed-demo` | Add sample data if the database is empty |

### Configuration

Settings live in `config/app.properties`. Missing keys fall back to built-in defaults.

| Key | Default | Purpose |
|---|---|---|
| `app.name` | Library Management System | Window title |
| `db.url` | `jdbc:sqlite:data/library.db` | Database location |
| `log.level` | `INFO` | Minimum level written to the console and `logs/` |
| `log.dir` | `logs` | Log folder |
| `member.borrowLimit` | `5` | Default borrow limit for new members |
| `reservation.holdDays` | `3` | How long a returned copy is held for the next person in the queue |
| `loan.maxRenewals` | `1` | How many times one loan can be renewed |
| `fine.blockLimit` | `100` | Unpaid fines (in rupees) at which a member cannot borrow. `0` turns the rule off |
| `backup.dir` | `data/backups` | Where database backups are written |
| `export.dir` | `data/exports` | Starting folder for file dialogs |
| `settings.file` | `data/settings.ser` | Saved window state and theme |

## Project layout

```
src/main/java/com/ucs553/library
  model/      domain classes: Item (Book, Magazine, DigitalResource), Member, Staff, Loan,
              Reservation, Fine, Notification, AuditEntry
  dao/        Database, Dao<T>, AbstractJdbcDao<T>, the JDBC implementations, transactions
  service/    LibraryService, AuthService, MemberPortal, PasswordHasher, demo data
  fine/       FinePolicy and the per-day implementation
  io/         CSV reading and writing, exports and imports
  config/     properties loading, logging setup, saved settings
  ui/         JavaFX screens and dialogs, one style.css for both themes
  exception/  checked exception hierarchy
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the layers fit together.

## Tests

`mvn clean test` runs more than 120 JUnit tests. They cover the domain model, DAOs against a temporary SQLite database, services (borrowing, the reservation queue, fines, renewals, member accounts and the member portal), schema migration, import and export, password hashing and saved settings. The screens are checked by hand.

## Known limitations

- Role checks for staff are made in the user interface. There is no separate server, so the service layer does not repeat them. Members are different: `MemberPortal` checks that every loan or reservation a member touches is theirs.
- Notifications are shown inside the application only. Nothing is sent by email or SMS.
- Fines are paid at the desk. There is no online payment.
- Dialog boxes (alerts, add and edit forms) keep the light look when dark mode is on.
- SQLite is the only supported database.
