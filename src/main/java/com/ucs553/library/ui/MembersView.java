package com.ucs553.library.ui;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.LoanStatus;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MembersView extends BorderPane implements Refreshable {

    private record LoanRow(String title, LocalDate issued, LocalDate due, LoanStatus status, BigDecimal fine) {
    }

    private static final Logger LOG = Logger.getLogger(MembersView.class.getName());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<Member> data = FXCollections.observableArrayList();
    private final FilteredList<Member> filtered = new FilteredList<>(data, m -> true);
    private final ListView<Member> list = new ListView<>();
    private final TextField search = new TextField();

    private final ObservableList<LoanRow> loanRows = FXCollections.observableArrayList();
    private final Label name = new Label();
    private final Label email = new Label();
    private final Label since = new Label();
    private final Label activeCount = new Label();

    public MembersView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        search.setPromptText("Search by name or email");
        search.setPrefWidth(300);
        Button add = new Button("Add member");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        add.getStyleClass().add("primary");
        edit.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(e -> addMember());
        edit.setOnAction(e -> editSelected());
        delete.setOnAction(e -> deleteSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, search, spacer, add, edit, delete);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        setTop(toolbar);

        buildList();
        SplitPane split = new SplitPane(list, buildDetail());
        split.setDividerPositions(0.35);
        setCenter(split);

        search.textProperty().addListener((obs, old, text) -> {
            String q = text.trim().toLowerCase();
            filtered.setPredicate(m -> q.isEmpty()
                    || m.getName().toLowerCase().contains(q)
                    || m.getEmail().contains(q));
            updateStatus();
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, member) -> showMember(member));
    }

    private void buildList() {
        list.setItems(new SortedList<>(filtered, Comparator.naturalOrder()));
        list.setPlaceholder(new Label("No members to show"));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                setText(empty || member == null ? null : member.getName() + "\n" + member.getEmail());
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                editSelected();
            }
        });
    }

    private StackPane buildDetail() {
        name.getStyleClass().add("member-name");
        email.getStyleClass().add("muted");
        since.getStyleClass().add("muted");
        activeCount.getStyleClass().add("section-title");

        TableView<LoanRow> table = new TableView<>(loanRows);
        TableColumn<LoanRow, String> item = new TableColumn<>("Item");
        item.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().title()));
        item.setPrefWidth(220);
        TableColumn<LoanRow, String> issued = new TableColumn<>("Issued");
        issued.setCellValueFactory(c -> new ReadOnlyStringWrapper(DATE.format(c.getValue().issued())));
        TableColumn<LoanRow, String> due = new TableColumn<>("Due");
        due.setCellValueFactory(c -> new ReadOnlyStringWrapper(DATE.format(c.getValue().due())));
        TableColumn<LoanRow, String> state = new TableColumn<>("Status");
        state.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().status() == LoanStatus.OVERDUE ? "Overdue" : "On loan"));
        TableColumn<LoanRow, String> fine = new TableColumn<>("Fine");
        fine.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().fine().signum() > 0 ? "₹" + c.getValue().fine() : "-"));
        table.getColumns().add(item);
        table.getColumns().add(issued);
        table.getColumns().add(due);
        table.getColumns().add(state);
        table.getColumns().add(fine);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No active loans"));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LoanRow row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("overdue");
                if (!empty && row != null && row.status() == LoanStatus.OVERDUE) {
                    getStyleClass().add("overdue");
                }
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox detail = new VBox(6, name, email, since, new Label(), activeCount, table);
        detail.setPadding(new Insets(16));
        detail.visibleProperty().bind(list.getSelectionModel().selectedItemProperty().isNotNull());

        Label hint = new Label("Select a member to see their loans");
        hint.getStyleClass().add("muted");
        hint.visibleProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        return new StackPane(hint, detail);
    }

    @Override
    public void refresh() {
        Member previous = list.getSelectionModel().getSelectedItem();
        int previousId = previous == null ? 0 : previous.getId();
        try {
            data.setAll(service.allMembers());
            updateStatus();
            if (previousId != 0) {
                data.stream().filter(m -> m.getId() == previousId).findFirst()
                        .ifPresent(m -> list.getSelectionModel().select(m));
            }
        } catch (LibraryException e) {
            fail("Could not load the members", e);
        }
    }

    private void showMember(Member member) {
        loanRows.clear();
        if (member == null) {
            return;
        }
        name.setText(member.getName());
        email.setText(member.getEmail());
        since.setText("Member since " + DATE.format(member.getJoinedOn()));
        try {
            List<Loan> loans = service.activeLoansOf(member.getId());
            LocalDate today = service.today();
            for (Loan loan : loans) {
                String title = service.findItem(loan.getItemId()).map(Item::getTitle).orElse("(removed item)");
                loanRows.add(new LoanRow(title, loan.getIssuedOn(), loan.getDueOn(),
                        loan.statusOn(today), service.currentFine(loan)));
            }
            activeCount.setText("Active loans: " + loans.size() + " of " + member.getBorrowLimit());
        } catch (LibraryException e) {
            fail("Could not load this member's loans", e);
        }
    }

    private void updateStatus() {
        status.set(filtered.size() + " of " + data.size() + " members");
    }

    private void addMember() {
        new MemberDialog(window(), null).showAndWait().ifPresent(this::save);
    }

    private void editSelected() {
        Member selected = list.getSelectionModel().getSelectedItem();
        if (selected != null) {
            new MemberDialog(window(), selected).showAndWait().ifPresent(this::save);
        }
    }

    private void deleteSelected() {
        Member selected = list.getSelectionModel().getSelectedItem();
        if (selected == null || !Dialogs.confirm(window(), "Delete " + selected.getName() + "?")) {
            return;
        }
        try {
            service.deleteMember(selected.getId());
            refresh();
        } catch (LibraryException e) {
            fail("This member could not be deleted. They may still have loan records.", e);
        }
    }

    private void save(Member member) {
        try {
            service.saveMember(member);
            refresh();
        } catch (DataAccessException e) {
            fail("Could not save the member", e);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void fail(String message, Exception e) {
        LOG.log(Level.WARNING, message, e);
        Dialogs.error(window(), message);
    }

    private Window window() {
        return getScene().getWindow();
    }
}
