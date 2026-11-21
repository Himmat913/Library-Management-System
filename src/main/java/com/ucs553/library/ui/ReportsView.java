package com.ucs553.library.ui;

import com.ucs553.library.AppContext;
import com.ucs553.library.Session;
import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.io.DataExporter;
import com.ucs553.library.io.DataImporter;
import com.ucs553.library.io.ImportResult;
import com.ucs553.library.service.FineDetail;
import com.ucs553.library.service.PopularItem;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
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

    private enum Report {
        OVERDUE("Overdue"), POPULAR("Most borrowed"), FINES("Outstanding fines");

        private final String label;

        Report(String label) {
            this.label = label;
        }
    }

    private static final Logger LOG = Logger.getLogger(ReportsView.class.getName());
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppContext context = AppContext.get();
    private final DataExporter exporter = new DataExporter(context.service());
    private final DataImporter importer = new DataImporter(context.service());
    private final StringProperty status;
    private final TextArea preview = new TextArea();
    private final Label previewTitle = heading("Overdue report");
    private Report shown = Report.OVERDUE;

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
        if (Session.isAdmin()) {
            actions.getChildren().add(heading("Import"));
            actions.getChildren().add(button("Items from CSV", e -> importFrom("Import items", true)));
            actions.getChildren().add(button("Members from CSV", e -> importFrom("Import members", false)));
            actions.getChildren().add(heading("Backup"));
            actions.getChildren().add(button("Back up database now", e -> backup()));
        }
        setLeft(actions);

        ToggleGroup group = new ToggleGroup();
        HBox segments = new HBox();
        for (Report report : Report.values()) {
            ToggleButton button = new ToggleButton(report.label);
            button.setToggleGroup(group);
            button.getStyleClass().add("segment");
            button.selectedProperty().addListener((obs, was, now) -> {
                if (now) {
                    shown = report;
                    refresh();
                }
            });
            segments.getChildren().add(button);
            if (report == Report.OVERDUE) {
                button.setSelected(true);
            }
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });
        Button save = new Button("Save this report");
        save.setOnAction(e -> exportTo("Save report", "report.txt", "Text files", "txt", this::saveShown));

        preview.setEditable(false);
        preview.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 13px;");
        VBox.setVgrow(preview, Priority.ALWAYS);
        HBox top = new HBox(16, segments, save);
        setCenter(new VBox(8, Pages.header("Reports", "Overdue items, popular items and unpaid fines. Export raw data on the left."),
                top, previewTitle, preview));
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
            previewTitle.setText(shown.label);
            preview.setText(switch (shown) {
                case OVERDUE -> exporter.overdueReport();
                case POPULAR -> popularReport();
                case FINES -> finesReport();
            });
            status.set("Reports");
        } catch (LibraryException e) {
            fail("Could not build the report", e);
        }
    }

    private String popularReport() throws LibraryException {
        StringBuilder text = new StringBuilder("Most borrowed items\n\n");
        int rank = 1;
        for (PopularItem item : context.service().mostBorrowed(15)) {
            text.append(String.format("%2d. %-45s %-25s %d loans%n", rank++, item.title(), item.author(), item.loans()));
        }
        if (rank == 1) {
            text.append("No loans yet.\n");
        }
        return text.toString();
    }

    private String finesReport() throws LibraryException {
        StringBuilder text = new StringBuilder("Outstanding fines\n\n");
        BigDecimal total = BigDecimal.ZERO;
        for (FineDetail detail : context.service().fineDetails(true)) {
            text.append(String.format("%-24s %-40s %10s   charged %s%n", detail.memberName(), detail.itemTitle(),
                    Pages.rupees(detail.fine().getAmount()), Pages.DATE.format(detail.fine().getCreatedOn())));
            total = total.add(detail.fine().getAmount());
        }
        text.append("\nTotal outstanding: ").append(Pages.rupees(total)).append("\n");
        return text.toString();
    }

    private void saveShown(Path file) throws IOException {
        Files.writeString(file, preview.getText());
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
