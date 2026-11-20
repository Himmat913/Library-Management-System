# Architecture

## Layers

```
ui (JavaFX screens)
   |
service (LibraryService, AuthService)
   |
dao (Dao<T>, Jdbc*Dao)
   |
Database (ConnectionSource)  ->  SQLite
```

Each layer only calls the one below it. Screens never touch SQL, and DAOs never know about screens.

## Packages

| Package | Responsibility |
|---|---|
| `model` | Plain domain classes. `Item` is abstract with `Book`, `Magazine` and `DigitalResource`. `Person` is the base of `Member` and `Staff`. `Loan` derives its status from dates. |
| `dao` | `Database` hands out connections and creates the schema. `AbstractJdbcDao<T>` implements insert, update, delete and find once. The concrete DAOs add their own queries and row mapping. |
| `service` | `LibraryService` is the facade the screens use for borrowing, returning, fines and catalogue maintenance. `AuthService` handles sign-in and staff accounts. |
| `fine` | `FinePolicy` is the strategy. `PerDayFinePolicy` holds one daily rate per item type. |
| `io` | `Csv`, `DataExporter`, `DataImporter`. |
| `config` | `AppConfig` (properties), `LogSetup` (java.util.logging), `SettingsStore` and `UserSettings` (object serialization). |
| `ui` | `MainView` with one page per screen, plus dialogs. |

## Data model

| Table | Notes |
|---|---|
| `items` | One table for all item types. A `type` column selects the subclass, and columns that do not apply to a type hold defaults. |
| `members` | Unique email. |
| `loans` | Foreign keys to `items` and `members`. `returned_on` is null while the loan is open. |
| `staff` | Unique username and email, PBKDF2 password hash, role. |

Dates are stored as ISO text. Fines are `BigDecimal` and are calculated, not stored. Overdue status is derived from the due date and today's date.

## Design decisions

- **Derived status.** A loan is overdue when it is open and its due date has passed. Storing the status would need a job to keep it correct.
- **Clock injection.** `LibraryService` takes a `java.time.Clock`, so tests can move time forward and check fines exactly.
- **One connection per operation.** DAOs open and close a connection for each call. This keeps them stateless and is fast enough for SQLite on a desktop.
- **Foreign keys enforced.** Every connection turns on SQLite foreign key checks, so loans cannot point at missing items or members, and items or members with loans cannot be deleted.

## Patterns used

| Pattern | Where |
|---|---|
| DAO (Business Object, DAO, Data Source, Transfer Object) | `service`, `dao`, `Database`, domain classes |
| Template Method | `AbstractJdbcDao` |
| Strategy | `FinePolicy` |
| Factory | `ItemFactory` |
| Builder | `Book.Builder` |
| Observer | `LoanListener`, `LoanEvent` |
| Facade | `LibraryService` |
| Singleton | `AppContext`, `Session` |

## Borrowing a book

1. `CirculationView` calls `LibraryService.borrow(itemId, memberId)`.
2. The service loads the item and member through their DAOs, and checks the member's active loan count against their limit.
3. `Item.checkOut()` reduces the available copies, and the item is saved.
4. A `Loan` is created with a due date from the item type and saved. If this fails, the copy is put back.
5. A `LoanEvent` is sent to listeners, and the open screens refresh.

## Errors and logging

Failures the user can fix are checked exceptions under `LibraryException`: item unavailable, borrow limit reached, not found, bad input. Database failures are wrapped in `DataAccessException`. Screens show the message in a dialog and write the detail to the log.

Logging uses `java.util.logging` with a console handler and a rotating file handler in `logs/`.
