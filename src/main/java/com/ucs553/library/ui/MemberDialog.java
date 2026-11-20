package com.ucs553.library.ui;

import com.ucs553.library.model.Member;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;

class MemberDialog extends Dialog<Member> {

    private final Member existing;
    private final TextField name = new TextField();
    private final TextField email = new TextField();
    private final TextField limit = new TextField(String.valueOf(Member.DEFAULT_BORROW_LIMIT));
    private final Label error = new Label();
    private Member result;

    MemberDialog(Window owner, Member existing) {
        this.existing = existing;
        initOwner(owner);
        setTitle(existing == null ? "Add member" : "Edit member");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Name"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(email, 1, 1);
        grid.add(new Label("Borrow limit"), 0, 2);
        grid.add(limit, 1, 2);
        name.setPrefWidth(280);
        email.setPrefWidth(280);
        limit.setPrefWidth(280);

        error.setStyle("-fx-text-fill: #b91c1c;");
        error.setWrapText(true);
        VBox content = new VBox(12, grid, error);
        content.setPadding(new Insets(16));
        content.setPrefWidth(420);
        getDialogPane().setContent(content);

        if (existing != null) {
            name.setText(existing.getName());
            email.setText(existing.getEmail());
            limit.setText(String.valueOf(existing.getBorrowLimit()));
        }

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                result = build();
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> button == ButtonType.OK ? result : null);
    }

    private Member build() {
        int max;
        try {
            max = Integer.parseInt(limit.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Borrow limit must be a whole number");
        }
        if (max < 1 || max > 50) {
            throw new IllegalArgumentException("Borrow limit must be between 1 and 50");
        }
        LocalDate joined = existing == null ? LocalDate.now() : existing.getJoinedOn();
        Member member = new Member(name.getText(), email.getText(), joined, max);
        if (existing != null) {
            member.setId(existing.getId());
        }
        return member;
    }
}
