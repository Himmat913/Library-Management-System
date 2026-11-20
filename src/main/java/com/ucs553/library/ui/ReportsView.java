package com.ucs553.library.ui;

import com.ucs553.library.AppContext;
import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.io.DataExporter;
import com.ucs553.library.io.DataImporter;
import com.ucs553.library.io.ImportResult;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportsView extends BorderPane implements Refreshable {

    @FunctionalInterface
    private interface FileAction {
        void run(Path file) throws IOException, LibraryException;
    }

    private static final Logger LOG = Logger.getLogger(ReportsView.class.getName());
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppContext context = AppContext.get();
    private final DataExporter exporter = new DataExporter(context.service());
    private final DataImporter importer = new DataImporter(context.service());
    private final StringProperty status;
    private final TextArea preview = new TextArea();

    public ReportsView(StringProperty status) {
        this.status = status;
        setPadding(new Insets(16));

        VBox actions = new VBox(8);
        actions.setPrefWidth(260);
        actions.setPadding(new Insets(0, 16, 0, 0));
        actions.getChildren().add(heading("Export"));
        actions.getChildren().add(button("Items (CSV)",
                e -> exportTo("Export items", "items.csv", "CSV files", "csv", exporter::exportItems)));
        actions.getChildren().add(button("Members (CSV)",
                e -> exportTo("Export members", "members.csv", "CSV files", "csv", exporter::exportMembers)));
        actions.getChildren().add(button("Loans (CSV)",
                e -> exportTo("Export loans", "loans.csv", "CSV files", "csv", exporter::exportLoans)));
        actions.getChildren().add(button("Overdue report (text)",
                e -> exportTo("Save overdue report", "overdue-report.txt", "Text files", "txt", exporter::writeOverdueReport)));
        actions.getChildren().add(heading("Import"));
        actions.getChildren().add(button("Items from CSV", e -> importFrom("Import items", true)));
        actions.getChildren().add(button("Members from CSV", e -> importFrom("Import members", false)));
        actions.getChildren().add(heading("Backup"));
        actions.getChildren().add(button("Back up database now", e -> backup()));
        setLeft(actions);

        Label previewTitle = heading("Overdue report preview");
        preview.setEditable(false);
        preview.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 13px;");
        VBox.setVgrow(preview, Priority.ALWAYS);
        setCenter(new VBox(8, previewTitle, preview));
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        label.setPadding(new Insets(10, 0, 0, 0));
        return label;
    }

    private Button button(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(handler);
        return button;
    }

    @Override
    public void refresh() {
        try {
            preview.setText(exporter.overdueReport());
            status.set("Reports");
        } catch (LibraryException e) {
            fail("Could not build the report", e);
        }
    }

    private void exportTo(String title, String fileName, String description, String extension, FileAction action) {
        FileChooser chooser = chooser(title);
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + extension));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            action.run(file.toPath());
            status.set("Saved " + file.getAbsolutePath());
        } catch (IOException e) {
            fail("Could not write the file: " + e.getMessage(), e);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void importFrom(String title, boolean items) {
        FileChooser chooser = chooser(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            ImportResult result = items ? importer.importItems(file.toPath()) : importer.importMembers(file.toPath());
            status.set("Imported " + result.imported() + (items ? " items" : " members"));
            Dialogs.info(window(), summary(result));
        } catch (IOException e) {
            fail("Could not read the file: " + e.getMessage(), e);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private String summary(ImportResult result) {
        StringBuilder text = new StringBuilder("Imported " + result.imported() + " rows.");
        if (result.hasErrors()) {
            text.append("\n\n").append(result.errors().size()).append(" rows were skipped:");
            result.errors().stream().limit(10).forEach(error -> text.append("\n").append(error));
            if (result.errors().size() > 10) {
                text.append("\n... and ").append(result.errors().size() - 10).append(" more");
            }
        }
        return text.toString();
    }

    private void backup() {
        Path target = Path.of(context.config().getString("backup.dir"))
                .resolve("library-" + LocalDateTime.now().format(STAMP) + ".db");
        try {
            context.database().backupTo(target);
            status.set("Backup saved to " + target.toAbsolutePath());
            Dialogs.info(window(), "Backup saved to\n" + target.toAbsolutePath());
        } catch (DataAccessException e) {
            fail("Could not back up the database", e);
        }
    }

    private FileChooser chooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        File dir = new File(context.config().getString("export.dir")).getAbsoluteFile();
        if (dir.isDirectory() || dir.mkdirs()) {
            chooser.setInitialDirectory(dir);
        }
        return chooser;
    }

    private void fail(String message, Exception e) {
        LOG.log(Level.WARNING, message, e);
        Dialogs.error(window(), message);
    }

    private Window window() {
        return getScene().getWindow();
    }
}
