package com.ucs553.library.ui;

import java.util.List;

final class ConceptCatalog {

    record Concept(String name, String where) {
    }

    private ConceptCatalog() {
    }

    static List<Concept> all() {
        return List.of(
                new Concept("Classes, inheritance, abstract classes", "Item with Book, Magazine, DigitalResource; Person with Member, Staff"),
                new Concept("Interfaces and default methods", "Borrowable, Dao, FinePolicy, LoanListener"),
                new Concept("Polymorphism", "Item.describe() on the Items screen; getType() in each subclass"),
                new Concept("Records and enums", "LoanEvent, ReturnResult, LoanDetail; ItemType, LoanStatus, Role"),
                new Concept("Generics", "Dao<T>, AbstractJdbcDao<T>, RowMapper<R>, generic page method in MainView"),
                new Concept("Collections", "EnumMap in PerDayFinePolicy, HashSet in DataImporter, Map and List across the services"),
                new Concept("Comparable and Comparator", "Item and Member sorting, overdue report ordering"),
                new Concept("Exceptions", "LibraryException hierarchy, try/catch/finally, try-with-resources"),
                new Concept("Lambdas and streams", "Binder and RowMapper in the DAOs, stream pipelines in the services and exports"),
                new Concept("Date and time API", "LocalDate, Clock, DateTimeFormatter for due dates and fines"),
                new Concept("BigDecimal for money", "PerDayFinePolicy and fine totals"),
                new Concept("File I/O", "CSV import and export, overdue report, Files and Path"),
                new Concept("Object serialization", "UserSettings saved with ObjectOutputStream, transient field, input filter"),
                new Concept("Properties files", "AppConfig reading config/app.properties"),
                new Concept("Logging", "java.util.logging with file rotation in LogSetup"),
                new Concept("JDBC and the DAO pattern", "Database as data source, Jdbc*Dao classes, PreparedStatement, domain objects as transfer objects"),
                new Concept("Design patterns", "Singleton (AppContext), Observer (LoanListener), Template Method (AbstractJdbcDao), "
                        + "Strategy (FinePolicy), Factory (ItemFactory), Builder (Book.Builder), Facade (LibraryService)"),
                new Concept("JavaFX", "TableView, ListView, ComboBox, dialogs, property binding, FilteredList and SortedList, CSS"),
                new Concept("Security basics", "PBKDF2 password hashing, roles, signed-in session"),
                new Concept("Unit testing", "JUnit tests for the model, DAOs, services, import and export, and settings"));
    }
}
