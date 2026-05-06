package Controllers.event;

import Models.Event;
import Services.EventService;
import Services.UserJoinedEventService;
import Utils.QRCodeGenerator;
import Utils.Session;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserJoinedEventsController {

    @FXML private FlowPane flowPaneEvents;

    private EventService eventService;
    private UserJoinedEventService joinedService;

    private UUID getCurrentUserId() {
        if (Session.getInstance().isLoggedIn()) {
            return Session.getInstance().getCurrentUser().getId();
        }
        String hex = "019cb4228d417e87a229dbd3a7e7b872";
        long high = Long.parseUnsignedLong(hex.substring(0, 16), 16);
        long low  = Long.parseUnsignedLong(hex.substring(16, 32), 16);
        return new UUID(high, low);
    }

    @FXML
    public void initialize() {
        eventService  = new EventService();
        joinedService = new UserJoinedEventService();
        loadJoinedEvents();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load & Render
    // ─────────────────────────────────────────────────────────────────────────

    private void loadJoinedEvents() {
        try {
            UUID userId = getCurrentUserId();
            List<Integer> joinedIds = joinedService.getJoinedEventIds(userId);
            List<Event>   allEvents = eventService.recuperer();

            List<Event> myTickets = allEvents.stream()
                    .filter(e -> joinedIds.contains(e.getId()))
                    .collect(Collectors.toList());

            flowPaneEvents.getChildren().clear();

            if (myTickets.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setStyle("-fx-alignment: center; -fx-padding: 60;");
                Label icon = new Label("🎟️");
                icon.setStyle("-fx-font-size: 48;");
                Label msg = new Label("Vous n'êtes inscrit à aucun événement.");
                msg.setStyle("-fx-font-style: italic; -fx-text-fill: #94a3b8; -fx-font-size: 16;");
                empty.getChildren().addAll(icon, msg);
                flowPaneEvents.getChildren().add(empty);
                return;
            }

            for (Event event : myTickets) {
                flowPaneEvents.getChildren().add(createTicketCard(event));
            }

        } catch (SQLDataException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ticket Card
    // ─────────────────────────────────────────────────────────────────────────

    private VBox createTicketCard(Event event) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12; " +
                "-fx-background-color: rgba(16, 185, 129, 0.05);");
        card.setPrefWidth(230);

        // Event image
        ImageView imgView = new ImageView();
        imgView.setFitWidth(190);
        imgView.setFitHeight(120);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(190, 120);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgView.setClip(clip);
        if (event.getImage() != null && !event.getImage().isEmpty()) {
            try { imgView.setImage(new Image(event.getImage(), true)); } catch (Exception ignored) {}
        }

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: white;");
        title.setWrapText(true);

        Label date = new Label("📅 " + event.getEventDate());
        date.setStyle("-fx-text-fill: #94a3b8;");

        Label badge = new Label("✅ Inscrit");
        badge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; " +
                "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11; -fx-font-weight: bold;");

        // ── Download Ticket button ──
        Button btnTicket = new Button("📥 Télécharger Ticket");
        btnTicket.setStyle("-fx-background-color: linear-gradient(to right,#0d9488,#10b981); " +
                "-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 15; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        btnTicket.setMaxWidth(Double.MAX_VALUE);
        btnTicket.setOnAction(e -> openTicketModal(event));

        // ── Details button ──
        Button btnDetails = new Button("Voir Détails");
        btnDetails.setStyle("-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #60a5fa; " +
                "-fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-font-weight: bold; " +
                "-fx-border-color: rgba(59,130,246,0.3); -fx-border-radius: 8;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openDetails(event));

        // ── Leave button ──
        Button btnLeave = new Button("Se désinscrire");
        btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.12); -fx-text-fill: #f87171; " +
                "-fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-font-weight: bold; " +
                "-fx-border-color: rgba(248,113,113,0.3); -fx-border-radius: 8;");
        btnLeave.setMaxWidth(Double.MAX_VALUE);
        btnLeave.setOnMouseEntered(e -> btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.25); " +
                "-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-padding: 8 15; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; " +
                "-fx-border-color: rgba(248,113,113,0.5); -fx-border-radius: 8;"));
        btnLeave.setOnMouseExited(e -> btnLeave.setStyle("-fx-background-color: rgba(248,113,113,0.12); " +
                "-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-padding: 8 15; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; " +
                "-fx-border-color: rgba(248,113,113,0.3); -fx-border-radius: 8;"));
        btnLeave.setOnAction(e -> confirmLeave(event));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgView, badge, title, date, spacer, btnTicket, btnDetails, btnLeave);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ticket Modal with QR Code
    // ─────────────────────────────────────────────────────────────────────────

    private void openTicketModal(Event event) {
        UUID userId = getCurrentUserId();
        Integer seatNumber = joinedService.getSeatNumber(userId, event.getId());

        String userName = Session.getInstance().isLoggedIn()
                ? Session.getInstance().getCurrentUser().getUsername()
                : "Participant";

        String qrData = QRCodeGenerator.buildTicketData(
                event.getId(), userId.toString(),
                event.getTitle(), event.getEventDate().toString(), seatNumber);

        // ── Modal window ──
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Votre Ticket");

        // ── Ticket card (snapshotted for download) ──
        VBox ticketCard = buildTicketCard(event, userName, seatNumber, qrData);

        // ── Save button ──
        Button btnSave = new Button("💾  Sauvegarder en PNG");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right,#0d9488,#10b981); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; " +
                "-fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        btnSave.setOnAction(e -> saveTicketAsPng(ticketCard, event.getTitle(), modal));

        Button btnClose = new Button("✕  Fermer");
        btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #94a3b8; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 10 25; " +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());

        HBox actions = new HBox(15, btnSave, btnClose);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(25, ticketCard, actions);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(10,15,30,0.96); -fx-padding: 40; " +
                "-fx-border-color: rgba(16,185,129,0.35); -fx-border-width: 1.5; " +
                "-fx-background-radius: 24; -fx-border-radius: 24;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }

    /** Builds the visual ticket card that will also be snapshot-saved as PNG. */
    private VBox buildTicketCard(Event event, String userName, Integer seatNumber, String qrData) {

        // Header gradient bar
        Pane headerBar = new Pane();
        headerBar.setPrefHeight(8);
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, #0d9488, #10b981, #34d399); " +
                "-fx-background-radius: 16 16 0 0;");

        // ── Logo + title row ──
        Label logoLbl = new Label("🎟️  SkillPath");
        logoLbl.setStyle("-fx-font-size: 22; -fx-font-weight: 900; -fx-text-fill: #10b981;");

        Label issuedLbl = new Label("TICKET D'ENTRÉE OFFICIEL");
        issuedLbl.setStyle("-fx-font-size: 9; -fx-font-weight: 900; -fx-text-fill: #475569; " +
                "-fx-letter-spacing: 2;");

        VBox logoBox = new VBox(2, logoLbl, issuedLbl);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        // QR code image
        Image qrImage = QRCodeGenerator.generateQRCode(qrData, 140, 140);
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(140);
        qrView.setFitHeight(140);
        Rectangle qrClip = new Rectangle(140, 140);
        qrClip.setArcWidth(10);
        qrClip.setArcHeight(10);
        qrView.setClip(qrClip);

        // ── Dashed separator ──
        Label dashes = new Label("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        dashes.setStyle("-fx-text-fill: #1e3a4a; -fx-font-size: 12;");

        // ── Event info ──
        Label evTitle = new Label(event.getTitle());
        evTitle.setStyle("-fx-font-size: 22; -fx-font-weight: 900; -fx-text-fill: white;");
        evTitle.setWrapText(true);

        String locationText = event.getLocation() != null
                ? event.getLocation().getName() + (event.getLocation().getBuilding() != null
                    ? " — " + event.getLocation().getBuilding() : "")
                : "Lieu à confirmer";

        Label[] infoRows = {
            infoRow("📅", "Date",        event.getEventDate().toString()),
            infoRow("⏰", "Heure",       event.getStartTime() + " — " + event.getEndTime()),
            infoRow("📍", "Lieu",        locationText),
            infoRow("👤", "Participant", userName),
            seatNumber != null ? infoRow("💺", "Siège N°",  String.valueOf(seatNumber))
                               : infoRow("🎫", "Accès",     "Places libres")
        };

        VBox infoBox = new VBox(8);
        for (Label row : infoRows) infoBox.getChildren().add(row);

        // ── Bottom: QR + info side by side ──
        HBox bottomRow = new HBox(30, infoBox, qrView);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // ── Assemble card ──
        VBox cardBody = new VBox(18);
        cardBody.setStyle("-fx-padding: 25 30 30 30;");
        cardBody.getChildren().addAll(logoBox, dashes, evTitle, bottomRow);

        VBox card = new VBox(0, headerBar, cardBody);
        card.setStyle("-fx-background-color: #0f2030; -fx-background-radius: 16; " +
                "-fx-border-color: rgba(16,185,129,0.25); -fx-border-radius: 16; -fx-border-width: 1;");
        card.setPrefWidth(520);
        card.setMaxWidth(520);

        return card;
    }

    /** Helper to build a two-column info row. */
    private Label infoRow(String icon, String label, String value) {
        Label lbl = new Label(icon + "  " + label + ":  " + value);
        lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");
        lbl.setWrapText(true);
        return lbl;
    }

    /** Snapshots the ticket card and saves it as a PNG using FileChooser. */
    private void saveTicketAsPng(VBox ticketCard, String eventTitle, Stage ownerStage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sauvegarder le ticket");
        fc.setInitialFileName("ticket_" + eventTitle.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fc.showSaveDialog(ownerStage);
        if (file == null) return;

        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = ticketCard.snapshot(params, null);
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);

            showSuccessToast(ownerStage, "✅  Ticket sauvegardé dans :\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
            showSuccessToast(ownerStage, "❌  Erreur lors de la sauvegarde.");
        }
    }

    /** Small toast overlay on the modal. */
    private void showSuccessToast(Stage ownerStage, String message) {
        Stage toast = new Stage();
        toast.initOwner(ownerStage);
        toast.initModality(Modality.NONE);
        toast.initStyle(StageStyle.TRANSPARENT);

        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold; " +
                "-fx-wrap-text: true; -fx-text-alignment: center;");
        lbl.setWrapText(true);

        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(16,185,129,0.9); -fx-padding: 18 28; " +
                "-fx-background-radius: 12;");
        box.setPrefWidth(380);

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        toast.setScene(scene);
        toast.show();

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(toast::close);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Leave Event
    // ─────────────────────────────────────────────────────────────────────────

    private void confirmLeave(Event event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; " +
                "-fx-border-color: rgba(248,113,113,0.4); -fx-border-width: 2; " +
                "-fx-background-radius: 20; -fx-border-radius: 20;");
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(380);

        Label icon    = new Label("🎟️");
        icon.setStyle("-fx-font-size: 44;");
        Label titleLbl = new Label("SE DÉSINSCRIRE ?");
        titleLbl.setStyle("-fx-text-fill: #f87171; -fx-font-weight: 900; -fx-font-size: 16; -fx-letter-spacing: 1;");
        Label msg = new Label("Voulez-vous annuler votre inscription à\n« " + event.getTitle() + " » ?");
        msg.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14; -fx-text-alignment: center;");
        msg.setWrapText(true);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);

        Button btnCancel = new Button("ANNULER");
        btnCancel.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #94a3b8; " +
                "-fx-font-weight: 900; -fx-padding: 12 28; -fx-background-radius: 10; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnConfirm = new Button("OUI, ME DÉSINSCRIRE");
        btnConfirm.setStyle("-fx-background-color: #f87171; -fx-text-fill: white; " +
                "-fx-font-weight: 900; -fx-padding: 12 20; -fx-background-radius: 10; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            try {
                joinedService.leaveEvent(getCurrentUserId(), event.getId());
                dialog.close();
                loadJoinedEvents();
            } catch (SQLDataException ex) {
                ex.printStackTrace();
                dialog.close();
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(icon, titleLbl, msg, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Open Details
    // ─────────────────────────────────────────────────────────────────────────

    private void openDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice/event/EventDetails.fxml"));
            Parent root = loader.load();
            UserEventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Event Details");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadJoinedEvents();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
