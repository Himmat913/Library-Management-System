# Architecture

## Layers

```
ui (JavaFX screens, one style.css)
   |
service (LibraryService, AuthService, MemberPortal)
   |
dao (Dao<T>, Jdbc*Dao, Repositories)
   |
Database (ConnectionSource, TransactionRunner)  ->  SQLite
```

Each layer only calls the one below it. Screens never touch SQL, and DAOs never know about screens. The screens are written in code, with no FXML.

## Packages

| Package | Responsibility |
|---|---|
| `model` | Plain domain classes. `Item` is abstract with `Book`, `Magazine` and `DigitalResource`. `Person` is the base of `Member` and `Staff`. `Loan` derives its status from dates. `Reservation`, `Fine`, `Notification`, `AuditEntry` and `MemberAccount` support the smart-library features. |
| `dao` | `Database` hands out connections, creates and migrates the schema, and runs work in a transaction. `AbstractJdbcDao<T>` implements insert, update, delete and find once. The concrete DAOs add their own queries and row mapping. `Repositories` bundles the DAOs the service needs. |
| `service` | `LibraryService` is the facade for borrowing, returning, renewing, reserving, fines, the activity log and catalogue maintenance. `AuthService` handles staff and member sign-in. `MemberPortal` is what a signed-in member is allowed to do. `DemoDataSeeder` builds the sample data. |
| `fine` | `FinePolicy` is the strategy. `PerDayFinePolicy` holds one daily rate per item type. |
| `io` | `Csv`, `DataExporter`, `DataImporter`. |
| `config` | `AppConfig` (properties), `LogSetup` (java.util.logging), `SettingsStore` and `UserSettings` (object serialization, including the theme). |
| `ui` | `MainView` (staff) and `MemberMainView` (members) with one page per screen, dialogs, `Pages` (shared headers, chips and empty states) and `Theme` (light and dark). |

## Data model

| Table | Notes |
|---|---|
| `items` | One table for all item types. A `type` column selects the subclass, and columns that do not apply to a type hold defaults. Has a `category`. |
| `members` | Unique email. |
| `loans` | Foreign keys to `items` and `members`. `returned_on` is null while the loan is open. `renew_count` counts renewals. |
| `reservations` | Member, item, status (waiting, ready, fulfilled, cancelled, expired) and the date a ready copy is held until. |
| `fines` | One row per late return, with the amount (as text, for exact decimals) and the date paid, if any. |
| `audit_log` | Time, who did it, what, and a sentence of detail. |
| `notifications` | In-app messages for a member, with a read flag. |
| `member_accounts` | One row per member who can sign in: password hash and a flag that forces a password change. Kept apart from `members` so ordinary member records never carry a hash. |
| `staff` | Unique username and email, PBKDF2 password hash, role. |

Dates are stored as ISO text. Overdue status is derived from the due date and today's date. Older databases are upgraded when the application starts: missing columns such as `items.category` and `loans.renew_count` are added in place.

## Design decisions

- **Derived loan status.** A loan is overdue when it is open and its due date has passed. Storing the status would need a job to keep it correct.
- **Stored fines.** Unlike the loan status, a fine has to survive after the item comes back, can be paid, and feeds the borrowing block, so it is a row.
- **Transactions.** `Database.run` sets auto-commit off and keeps the connection in a thread-local, so the DAOs called inside it share one connection. Everything commits together or rolls back on a failure. Borrow, return, renew, reserve, cancel and collect-fine each run as one transaction, together with their audit entry and notification.
- **The queue.** When a copy comes back, the first waiting reservation becomes ready and the copy is held for it for `reservation.holdDays`. It is not counted as available. If the hold lapses or is cancelled, the copy moves to the next person, or goes back on the shelf. Lapsed holds are released at the start of borrow, return and reserve.
- **Who did it.** `LibraryService` takes a `Supplier<String>` for the actor name, which the application supplies from `Session`. The service does not depend on the screens.
- **Clock injection.** `LibraryService` takes a `java.time.Clock`, so tests can move time forward and check fines and holds exactly.
- **One connection per operation.** Outside a transaction, DAOs open and close a connection for each call. This keeps them stateless and is fast enough for SQLite on a desktop.
- **Foreign keys enforced.** Every connection turns on SQLite foreign key checks, so loans cannot point at missing items or members, and items or members with loans cannot be deleted.
- **Members are checked in the service.** `MemberPortal` wraps the service for one member and refuses to act on a loan or reservation that is not theirs.
- **Themes with one stylesheet.** Colours are defined once as named values in `style.css`. Dark mode adds a `dark` class to the scene root, which swaps those values.

## Patterns used

| Pattern | Where |
|---|---|
| DAO (Business Object, DAO, Data Source, Transfer Object) | `service`, `dao`, `Database`, domain classes |
| Template Method | `AbstractJdbcDao` |
| Strategy | `FinePolicy` |
| Factory | `ItemFactory` |
| Builder | `Book.Builder` |
| Observer | `LoanListener`, `LoanEvent`, change listeners |
| Facade | `LibraryService` |
| Singleton | `AppContext`, `Session` |

## Borrowing a book

1. `CirculationView` calls `LibraryService.borrow(itemId, memberId)`.
2. The service starts a transaction and releases any holds that have lapsed.
3. It loads the item and member, refuses if the member's unpaid fines reach the block limit, and checks the active loan count against their limit.
4. If the member has a ready reservation for this item, the held copy is used and the reservation is marked fulfilled. Otherwise `Item.checkOut()` reduces the available copies.
5. A `Loan` is saved with a due date from the item type, and an audit entry is written.
6. The transaction commits. If any step failed, everything is rolled back.
7. Listeners are told, and the open screens refresh.

## Returning a book that someone is waiting for

1. `LibraryService.returnLoan(loanId)` marks the loan returned.
2. If it is late, a fine row is stored and logged.
3. The first waiting reservation becomes ready with a hold date, the member gets a notification, and the copy stays off the shelf. With nobody waiting, the copy goes back on the shelf.
4. The result tells the screen who the copy is being held for.

## Errors and logging

Failures the user can fix are checked exceptions under `LibraryException`: item unavailable, borrow limit reached, not found, bad input. Database failures are wrapped in `DataAccessException`. Screens show the message in a dialog and write the detail to the log.

Logging uses `java.util.logging` with a console handler and a rotating file handler in `logs/`. The activity log in the database is separate: it is for people to read, the log files are for developers.
