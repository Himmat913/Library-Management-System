package com.ucs553.library.ui;

import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Notification;
import com.ucs553.library.service.MemberPortal;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MemberNoticesView extends BorderPane implements Refreshable {

    private static final Logger LOG = Logger.getLogger(MemberNoticesView.class.getName());

    private final MemberPortal portal;
    private final StringProperty status;
    private final Runnable onRead;
    private final ListView<Notification> list = new ListView<>();

    public MemberNoticesView(MemberPortal portal, StringProperty status, Runnable onRead) {
        this.portal = portal;
        this.status = status;
        this.onRead = onRead;
        setPadding(new Insets(16));

        list.setPlaceholder(new Label("No notifications yet"));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Notification note, boolean empty) {
                super.updateItem(note, empty);
                getStyleClass().remove("unread");
                if (empty || note == null) {
                    setText(null);
                    return;
                }
                setText(Pages.DATE_TIME.format(note.getCreatedAt()) + "\n" + note.getMessage());
                setWrapText(true);
                if (!note.isRead()) {
                    getStyleClass().add("unread");
                }
            }
        });
        setTop(Pages.header("Notifications", "Messages from the library, such as a reserved item being ready."));
        setCenter(list);
    }

    @Override
    public void refresh() {
        try {
            list.getItems().setAll(portal.notifications());
            long unread = list.getItems().stream().filter(n -> !n.isRead()).count();
            status.set(unread == 0 ? "No new notifications" : unread + " new notifications");
            portal.markNotificationsRead();
            onRead.run();
        } catch (LibraryException e) {
            LOG.log(Level.WARNING, "Could not load the notifications", e);
            Dialogs.error(getScene().getWindow(), "Could not load your notifications");
        }
    }
}
