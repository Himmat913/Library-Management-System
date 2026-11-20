# Library Management System

A desktop library management application written in Java for the UCS553 (Enterprise Web Application) course. It uses a JavaFX interface and, as development progresses, a JDBC database layer built on the DAO pattern.

## Status

Early development. The repository currently contains the Maven build and an empty JavaFX window.

## Requirements

- JDK 25 (JavaFX 25 needs a recent JDK)
- Maven 3.9 or newer

## Build and run

```
mvn clean test
mvn javafx:run
```

Command-line options (pass them with `-Djavafx.args="..."` when using `javafx:run`):

| Option | Meaning |
|---|---|
| `--config <file>` | Use a different properties file (default `config/app.properties`) |
| `--reset-db` | Recreate the database on startup |
| `--seed-demo` | Load demo data on startup |

The configuration file and database are added in later phases, so these options are parsed but not used yet.
