# Library Management System

A desktop library management application written in Java for the UCS553 (Enterprise Web Application) course. It has a JavaFX interface, a SQLite database accessed through JDBC with the DAO pattern, staff sign-in with roles, loan and fine handling, and CSV import and export.

The project is also a demonstration of the course concepts: object-oriented design, generics, collections, exceptions, file handling, serialization, logging, JDBC and design patterns.

## Features

- **Circulation**: issue and return items, borrow limits, due dates, overdue highlighting and per-day fines by item type.
- **Items**: books, magazines and digital resources with add, edit, delete, search and sorting.
- **Members**: member list with search and a detail panel showing each member's active loans.
- **Reports**: export items, members and loans to CSV, an overdue report as text, CSV import with row-by-row error reporting, and a database backup.
- **Staff accounts**: sign-in with salted, hashed passwords. Administrators manage accounts and can delete records, import data and take backups. Librarians handle circulation and day-to-day edits.
- **Remembered state**: window size, last page and last username are saved between runs.

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

To start with demo data:

```
mvn javafx:run "-Djavafx.args=--reset-db --seed-demo"
```

### Command-line options

| Option | Meaning |
|---|---|
| `--config <file>` | Use a different properties file (default `config/app.properties`) |
| `--reset-db` | Drop and recreate all tables, including staff accounts |
| `--seed-demo` | Add sample items, members and loans if the database is empty |

### Configuration

Settings live in `config/app.properties`. Missing keys fall back to built-in defaults.

| Key | Default | Purpose |
|---|---|---|
| `app.name` | Library Management System | Window title |
| `db.url` | `jdbc:sqlite:data/library.db` | Database location |
| `log.level` | `INFO` | Minimum level written to the console and `logs/` |
| `log.dir` | `logs` | Log folder |
| `member.borrowLimit` | `5` | Default borrow limit for new members |
| `backup.dir` | `data/backups` | Where database backups are written |
| `export.dir` | `data/exports` | Starting folder for file dialogs |
| `settings.file` | `data/settings.ser` | Saved window state |

## Project layout

```
src/main/java/com/ucs553/library
  model/      domain classes: Item (Book, Magazine, DigitalResource), Member, Staff, Loan
  dao/        Database, Dao<T>, AbstractJdbcDao<T> and the JDBC implementations
  service/    LibraryService, AuthService, PasswordHasher, demo data
  fine/       FinePolicy and the per-day implementation
  io/         CSV reading and writing, exports and imports
  config/     properties loading, logging setup, saved settings
  ui/         JavaFX screens and dialogs
  exception/  checked exception hierarchy
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the layers fit together.

## Tests

`mvn clean test` runs the JUnit tests, which cover the domain model, DAOs against a temporary SQLite database, services, import and export, password hashing and saved settings. The screens are checked by hand.

## Known limitations

- Role checks are made in the user interface. There is no separate server, so the service layer does not repeat them.
- Issuing and returning update the item and the loan in two steps. A failure in the second step undoes the first, but the two are not one database transaction.
- SQLite is the only supported database.
- Loans cannot be renewed.
