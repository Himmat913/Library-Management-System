package com.ucs553.library.ui;

import com.ucs553.library.dao.DataAccessException;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Item;
import com.ucs553.library.model.Loan;
import com.ucs553.library.model.LoanStatus;
import com.ucs553.library.model.Member;
import com.ucs553.library.service.LibraryService;
import com.ucs553.library.service.LoanEvent;
import com.ucs553.library.service.ReturnResult;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CirculationView extends BorderPane implements Refreshable {

    private record LoanRow(Loan loan, String title, String member, LoanStatus status, BigDecimal fine) {
    }

    private enum Filter {
        ACTIVE("Active"), OVERDUE("Overdue"), RETURNED("Returned"), ALL("All");

        private final String label;

        Filter(String label) {
            this.label = label;
        }

        boolean accepts(LoanRow row) {
            return switch (this) {
                case ACTIVE -> row.status() != LoanStatus.RETURNED;
                case OVERDUE -> row.status() == LoanStatus.OVERDUE;
                case RETURNED -> row.status() == LoanStatus.RETURNED;
                case ALL -> true;
            };
        }
    }

    private static final Logger LOG = Logger.getLogger(CirculationView.class.getName());
    private static final int MAX_RECENT = 8;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LibraryService service;
    private final StringProperty status;
    private final ObservableList<LoanRow> rows = FXCollections.observableArrayList();
    private final FilteredList<LoanRow> filtered = new FilteredList<>(rows, r -> Filter.ACTIVE.accepts(r));
    private final TableView<LoanRow> table = new TableView<>();
    private final ComboBox<Member> memberBox = new ComboBox<>();
    private final ComboBox<Item> itemBox = new ComboBox<>();
    private final Label summary = new Label();
    private final CheckBox finesOnly = new CheckBox("With fines only");
    private final LinkedList<String> recent = new LinkedList<>();
    private final ListView<String> activity = new ListView<>();
    private Filter current = Filter.ACTIVE;

    public CirculationView(LibraryService service, StringProperty status) {
        this.service = service;
        this.status = status;
        setPadding(new Insets(16));

        setTop(new VBox(12, buildIssuePanel(), buildFilterBar()));
        buildTable();
        setCenter(table);
        BorderPane.setMargin(table, new Insets(12, 0, 0, 0));

        setRight(buildActivityPanel());

        service.addListener(event -> {
            recordActivity(event);
            refresh();
        });
    }

    private HBox buildIssuePanel() {
        memberBox.setPromptText("Choose a member");
        memberBox.setPrefWidth(260);
        itemBox.setPromptText("Choose an available item");
        itemBox.setPrefWidth(340);
        Button issue = new Button("Issue loan");
        issue.getStyleClass().add("primary");
        issue.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                issueLoan();
            }
        });

        HBox panel = new HBox(10, new Label("Member"), memberBox, new Label("Item"), itemBox, issue);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.getStyleClass().add("card");
        return panel;
    }

    private HBox buildFilterBar() {
        ToggleGroup group = new ToggleGroup();
        HBox segments = new HBox();
        for (Filter filter : Filter.values()) {
            ToggleButton button = new ToggleButton(filter.label);
            button.setToggleGroup(group);
            button.getStyleClass().add("segment");
            button.selectedProperty().addListener((obs, was, selected) -> {
                if (selected) {
                    current = filter;
                    applyFilter();
                }
            });
            segments.getChildren().add(button);
            if (filter == Filter.ACTIVE) {
                button.setSelected(true);
            }
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);
            }
        });

        Button giveBack = new Button("Return selected");
        giveBack.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            LoanRow row = table.getSelectionModel().getSelectedItem();
            return row == null || row.status() == LoanStatus.RETURNED;
        }, table.getSelectionModel().selectedItemProperty()));
        giveBack.setOnAction(e -> returnSelected());

        summary.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        finesOnly.selectedProperty().addListener((obs, was, now) -> applyFilter());
        HBox bar = new HBox(16, segments, finesOnly, summary, spacer, giveBack);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildActivityPanel() {
        activity.setPlaceholder(new Label("No activity yet this session"));
        activity.setPrefWidth(250);
        VBox panel = new VBox(6, new Label("Recent activity"), activity);
        panel.setPadding(new Insets(0, 0, 0, 12));
        VBox.setVgrow(activity, Priority.ALWAYS);
        return panel;
    }

    private void applyFilter() {
        filtered.setPredicate(row -> current.accepts(row) && (!finesOnly.isSelected() || row.fine().signum() > 0));
        updateStatus();
    }

    private void recordActivity(LoanEvent event) {
        String title = "item " + event.loan().getItemId();
        String who = "member " + event.loan().getMemberId();
        try {
            title = service.findItem(event.loan().getItemId()).map(Item::getTitle).orElse(title);
            who = service.findMember(event.loan().getMemberId()).map(Member::getName).orElse(who);
        } catch (LibraryException e) {
            LOG.log(Level.WARNING, "Could not describe the loan event", e);
        }
        String verb = event.type() == LoanEvent.Type.ISSUED ? " borrowed " : " returned ";
        recent.addFirst(who + verb + title);

        int seen = 0;
        Iterator<String> it = recent.iterator();
        while (it.hasNext()) {
            it.next();
            if (++seen > MAX_RECENT) {
                it.remove();
            }
        }
        activity.getItems().setAll(recent);
    }

    private void buildTable() {
        TableColumn<LoanRow, String> item = new TableColumn<>("Item");
        item.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().title()));
        item.setPrefWidth(240);

        TableColumn<LoanRow, String> member = new TableColumn<>("Member");
        member.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().member()));
        member.setPrefWidth(170);

        TableColumn<LoanRow, LocalDate> issued = dateColumn("Issued", r -> r.loan().getIssuedOn());
        TableColumn<LoanRow, LocalDate> due = dateColumn("Due", r -> r.loan().getDueOn());
        TableColumn<LoanRow, LocalDate> returned = dateColumn("Returned", r -> r.loan().getReturnedOn());

        TableColumn<LoanRow, String> state = new TableColumn<>("Status");
        state.setCellValueFactory(c -> new ReadOnlyStringWrapper(switch (c.getValue().status()) {
            case ACTIVE -> "On loan";
            case OVERDUE -> "Overdue";
            case RETURNED -> "Returned";
        }));

        TableColumn<LoanRow, String> fine = new TableColumn<>("Fine");
        fine.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().fine().signum() > 0 ? "₹" + c.getValue().fine() : "-"));

        table.getColumns().add(item);
        table.getColumns().add(member);
        table.getColumns().add(issued);
        table.getColumns().add(due);
        table.getColumns().add(returned);
        table.getColumns().add(state);
        table.getColumns().add(fine);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No loans to show"));

        SortedList<LoanRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().add(due);

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
    }

    private TableColumn<LoanRow, LocalDate> dateColumn(String title, Function<LoanRow, LocalDate> getter) {
        TableColumn<LoanRow, LocalDate> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new ReadOnlyObjectWrapper<LocalDate>(getter.apply(c.getValue())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty ? null : date == null ? "-" : DATE.format(date));
            }
        });
        return column;
    }

    @Override
    public void refresh() {
        try {
            List<Item> items = service.allItems();
            List<Member> members = service.allMembers();
            Map<Integer, Item> itemById = items.stream().collect(Collectors.toMap(Item::getId, Function.identity()));
            Map<Integer, Member> memberById = members.stream().collect(Collectors.toMap(Member::getId, Function.identity()));
            LocalDate today = service.today();

            List<LoanRow> built = new ArrayList<>();
            for (Loan loan : service.allLoans()) {
                Item item = itemById.get(loan.getItemId());
                Member member = memberById.get(loan.getMemberId());
                built.add(new LoanRow(loan,
                        item == null ? "(removed)" : item.getTitle(),
                        member == null ? "(removed)" : member.getName(),
                        loan.statusOn(today),
                        item == null ? BigDecimal.ZERO : service.fineFor(loan, item.getType())));
            }
            rows.setAll(built);

            memberBox.setItems(FXCollections.observableArrayList(members.stream().sorted().toList()));
            itemBox.setItems(FXCollections.observableArrayList(items.stream().filter(Item::isAvailable).sorted().toList()));
            updateSummary();
            updateStatus();
        } catch (LibraryException e) {
            fail("Could not load the loans", e);
        }
    }

    private void updateSummary() {
        long active = rows.stream().filter(r -> r.status() != LoanStatus.RETURNED).count();
        long overdue = rows.stream().filter(r -> r.status() == LoanStatus.OVERDUE).count();
        BigDecimal fines = rows.stream().filter(r -> r.status() == LoanStatus.OVERDUE)
                .map(LoanRow::fine).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setText(active + " active, " + overdue + " overdue, fines building up: ₹" + fines.setScale(2));
    }

    private void updateStatus() {
        status.set(filtered.size() + " of " + rows.size() + " loans shown");
    }

    private void issueLoan() {
        Member member = memberBox.getValue();
        Item item = itemBox.getValue();
        if (member == null || item == null) {
            Dialogs.error(window(), "Choose a member and an item first");
            return;
        }
        try {
            Loan loan = service.borrow(item.getId(), member.getId());
            status.set("Issued \"" + item.getTitle() + "\" to " + member.getName() + ", due " + DATE.format(loan.getDueOn()));
        } catch (DataAccessException e) {
            fail("Could not issue the loan", e);
        } catch (LibraryException e) {
            fail(e.getMessage(), e);
        }
    }

    private void returnSelected() {
        LoanRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        try {
            ReturnResult result = service.returnLoan(row.loan().getId());
            if (result.hasFine()) {
                Dialogs.info(window(), "Returned \"" + row.title() + "\".\nFine to collect: ₹" + result.fine());
            } else {
                status.set("Returned \"" + row.title() + "\", no fine");
            }
        } catch (DataAccessException e) {
            fail("Could not return the loan", e);
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
