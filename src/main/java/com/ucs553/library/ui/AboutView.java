package com.ucs553.library.ui;

import com.ucs553.library.AppContext;
import com.ucs553.library.Session;
import com.ucs553.library.config.AppConfig;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class AboutView extends BorderPane implements Refreshable {

    private final GridPane facts = new GridPane();

    public AboutView() {
        setPadding(new Insets(16));

        Label title = new Label("About this application");
        title.getStyleClass().add("member-name");
        facts.setHgap(16);
        facts.setVgap(4);
        facts.getStyleClass().add("card");

        Label conceptsTitle = new Label("Java concepts used and where");
        conceptsTitle.getStyleClass().add("section-title");

        TableView<ConceptCatalog.Concept> table = new TableView<>();
        TableColumn<ConceptCatalog.Concept, String> concept = new TableColumn<>("Concept");
        concept.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        concept.setPrefWidth(260);
        TableColumn<ConceptCatalog.Concept, String> where = new TableColumn<>("Where in the project");
        where.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().where()));
        table.getColumns().add(concept);
        table.getColumns().add(where);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getItems().addAll(ConceptCatalog.all());
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        VBox top = new VBox(10, title, facts, conceptsTitle);
        top.setPadding(new Insets(0, 0, 10, 0));
        setTop(top);
        setCenter(table);
    }

    @Override
    public void refresh() {
        AppConfig config = AppContext.get().config();
        facts.getChildren().clear();
        String[][] rows = {
                {"Application", config.getString("app.name") + " " + config.getString("app.version")},
                {"Signed in as", Session.current() == null ? "-" : Session.current().getName() + " (" + Session.current().getRole() + ")"},
                {"Java", System.getProperty("java.version")},
                {"JavaFX", System.getProperty("javafx.runtime.version", "unknown")},
                {"Operating system", System.getProperty("os.name")},
                {"Database", config.getString("db.url")},
                {"Log level", config.getString("log.level")}
        };
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0]);
            key.getStyleClass().add("muted");
            facts.add(key, 0, i);
            facts.add(new Label(rows[i][1]), 1, i);
        }
    }
}
