package Controllers.event;

import Controllers.user.admin.SideBarController;
import Models.Event;
import Services.EventService;
import Services.UserJoinedEventService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.*;

public class AdminEventAnalyticsController implements Initializable {

    @FXML private VBox dashboardContainer;
    @FXML private SideBarController sideBarController;

    private final EventService eventService = new EventService();
    private final UserJoinedEventService joinedService = new UserJoinedEventService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sideBarController != null) {
            sideBarController.setSelected("analytics");
        }
        buildDashboard();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard Builder
    // ─────────────────────────────────────────────────────────────────────────

    private void buildDashboard() {
        if (dashboardContainer == null) return;
        dashboardContainer.getChildren().clear();
        dashboardContainer.setSpacing(35);
        dashboardContainer.setPadding(new Insets(40));

        // ── Page Header ──
        dashboardContainer.getChildren().add(buildPageHeader());

        // ── KPI Cards Row ──
        dashboardContainer.getChildren().add(buildKpiRow());

        // ── Section: Top Popular Events ──
        dashboardContainer.getChildren().add(buildSectionTitle("🏆", "Événements les Plus Populaires",
                "Top 5 des événements avec le plus de participants"));
        dashboardContainer.getChildren().add(buildTopEventsChart());

        // ── Section: Capacity Utilization ──
        dashboardContainer.getChildren().add(buildSectionTitle("📦", "Utilisation de la Capacité",
                "Remplissage par rapport à la capacité maximale de chaque lieu"));
        dashboardContainer.getChildren().add(buildCapacityChart());

        // ── Row: Monthly Events + Rating Distribution ──
        HBox bottomRow = new HBox(25);
        VBox monthChart = new VBox(15);
        monthChart.getChildren().addAll(
                buildSectionTitle("📆", "Inscriptions par Mois", "Tendances des 12 derniers mois"),
                buildMonthlyChart());
        HBox.setHgrow(monthChart, Priority.ALWAYS);

        VBox ratingChart = new VBox(15);
        ratingChart.getChildren().addAll(
                buildSectionTitle("⭐", "Distribution des Notes", "Répartition des scores 1 à 5"),
                buildRatingChart());
        ratingChart.setPrefWidth(320);

        bottomRow.getChildren().addAll(monthChart, ratingChart);
        dashboardContainer.getChildren().add(bottomRow);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page Header
    // ─────────────────────────────────────────────────────────────────────────

    private HBox buildPageHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(header, new Insets(0, 0, 10, 0));

        Text t1 = new Text("Analytiques");
        t1.setStyle("-fx-font-size: 32; -fx-font-weight: 800; -fx-fill: white;");
        Text t2 = new Text(" Événements");
        t2.getStyleClass().add("gradient-text");
        t2.setStyle("-fx-font-size: 32; -fx-font-weight: 800;");

        Label sub = new Label("Tableau de bord des statistiques et indicateurs clés des événements.");
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14;");

        VBox texts = new VBox(6, new HBox(t1, t2), sub);
        header.getChildren().add(texts);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KPI Row
    // ─────────────────────────────────────────────────────────────────────────

    private HBox buildKpiRow() {
        int totalEvents       = eventService.getTotalEvents();
        int upcomingEvents    = eventService.getUpcomingEventsCount();
        int pastEvents        = eventService.getPastEventsCount();
        int totalParticipants = joinedService.getTotalParticipants();
        double avgRating      = eventService.getAverageRatingOverall();

        // Calculate attendance rate: totalParticipants / totalCapacity
        int totalCapacity = 0;
        try {
            List<Event> events = eventService.recuperer();
            for (Event e : events) {
                if (e.getLocation() != null && e.getLocation().getMaxCapacity() > 0) {
                    totalCapacity += e.getLocation().getMaxCapacity();
                }
            }
        } catch (SQLDataException ignored) {}

        double attendanceRate = totalCapacity > 0
                ? Math.min(100.0, (double) totalParticipants / totalCapacity * 100.0) : 0.0;

        HBox row = new HBox(20);
        row.getChildren().addAll(
                kpiCard("📅", String.valueOf(totalEvents),     "Total Événements",    "#8b5cf6"),
                kpiCard("📈", String.valueOf(totalParticipants),"Inscriptions Totales","#10b981"),
                kpiCard("🔜", String.valueOf(upcomingEvents),  "Événements À Venir",  "#3b82f6"),
                kpiCard("✅", String.valueOf(pastEvents),       "Événements Passés",   "#f59e0b"),
                kpiCard("📊", String.format("%.0f%%", attendanceRate), "Taux de Remplissage","#ec4899"),
                kpiCard("⭐", String.format("%.1f / 5", avgRating),    "Note Moyenne",        "#f97316")
        );
        for (javafx.scene.Node n : row.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        return row;
    }

    private VBox kpiCard(String icon, String value, String label, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04); " +
                "-fx-background-radius: 16; -fx-border-radius: 16; " +
                "-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1;");

        // Colored top bar
        Pane bar = new Pane();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 28;");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 28; -fx-font-weight: 900; -fx-text-fill: " + color + ";");

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        card.getChildren().addAll(bar, iconLbl, valLbl, nameLbl);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section Title Helper
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildSectionTitle(String icon, String title, String subtitle) {
        Label ttl = new Label(icon + "  " + title);
        ttl.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: white;");
        Label sub = new Label(subtitle);
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");
        return new VBox(3, ttl, sub);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Events Horizontal Bar Chart
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildTopEventsChart() {
        VBox chart = new VBox(12);
        chart.setPadding(new Insets(20));
        chart.setStyle(cardStyle());

        List<int[]> top = joinedService.getTopEvents(5);

        if (top.isEmpty()) {
            Label empty = new Label("Aucune inscription pour le moment.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14;");
            chart.getChildren().add(empty);
            return chart;
        }

        // Get event titles
        Map<Integer, String> titles = new HashMap<>();
        try {
            for (Event e : eventService.recuperer()) titles.put(e.getId(), e.getTitle());
        } catch (SQLDataException ignored) {}

        int maxCount = top.get(0)[1];

        String[] colors = {"#8b5cf6", "#3b82f6", "#10b981", "#f59e0b", "#ec4899"};

        for (int i = 0; i < top.size(); i++) {
            int eventId = top.get(i)[0];
            int count   = top.get(i)[1];
            String eventTitle = titles.getOrDefault(eventId, "Événement #" + eventId);
            String color = colors[i % colors.length];

            double pct = maxCount > 0 ? (double) count / maxCount : 0;

            Label nameLbl = new Label((i + 1) + ".  " + truncate(eventTitle, 40));
            nameLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13; -fx-font-weight: bold;");
            nameLbl.setPrefWidth(260);
            nameLbl.setMinWidth(260);

            // Background track
            StackPane track = new StackPane();
            track.setPrefHeight(28);
            track.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6;");
            HBox.setHgrow(track, Priority.ALWAYS);

            // Fill bar
            Rectangle fill = new Rectangle();
            fill.setHeight(28);
            fill.setArcWidth(6);
            fill.setArcHeight(6);
            fill.setFill(Color.web(color, 0.85));

            // Bind fill width to track width * percentage
            track.widthProperty().addListener((obs, ov, nv) ->
                    fill.setWidth(nv.doubleValue() * pct));

            StackPane fillPane = new StackPane(fill);
            fillPane.setAlignment(Pos.CENTER_LEFT);

            track.getChildren().add(fillPane);

            Label countLbl = new Label(count + " inscrit" + (count > 1 ? "s" : ""));
            countLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12; -fx-font-weight: bold;");
            countLbl.setPrefWidth(90);
            countLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(15, nameLbl, track, countLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            chart.getChildren().add(row);
        }
        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Capacity Utilization Chart
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildCapacityChart() {
        VBox chart = new VBox(12);
        chart.setPadding(new Insets(20));
        chart.setStyle(cardStyle());

        Map<Integer, Integer> participantMap = joinedService.getParticipantCountPerEvent();

        try {
            List<Event> events = eventService.recuperer();
            events.sort((a, b) -> {
                int ca = participantMap.getOrDefault(a.getId(), 0);
                int cb = participantMap.getOrDefault(b.getId(), 0);
                return Integer.compare(cb, ca);
            });

            int shown = 0;
            for (Event e : events) {
                if (shown >= 7) break;
                if (e.getLocation() == null || e.getLocation().getMaxCapacity() <= 0) continue;

                int capacity  = e.getLocation().getMaxCapacity();
                int joined    = participantMap.getOrDefault(e.getId(), 0);
                double pct    = (double) joined / capacity;

                String color = pct >= 0.9 ? "#ef4444"
                             : pct >= 0.6 ? "#f59e0b"
                             : "#10b981";

                Label nameLbl = new Label(truncate(e.getTitle(), 35));
                nameLbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13;");
                nameLbl.setPrefWidth(240);
                nameLbl.setMinWidth(240);

                StackPane track = new StackPane();
                track.setPrefHeight(26);
                track.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6;");
                HBox.setHgrow(track, Priority.ALWAYS);

                Rectangle fill = new Rectangle();
                fill.setHeight(26);
                fill.setArcWidth(6);
                fill.setArcHeight(6);
                fill.setFill(Color.web(color, 0.8));
                final double p = Math.min(pct, 1.0);
                track.widthProperty().addListener((obs, ov, nv) -> fill.setWidth(nv.doubleValue() * p));

                StackPane fillPane = new StackPane(fill);
                fillPane.setAlignment(Pos.CENTER_LEFT);
                track.getChildren().add(fillPane);

                Label pctLbl = new Label(joined + " / " + capacity + "  (" + String.format("%.0f%%", pct * 100) + ")");
                pctLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11; -fx-font-weight: bold;");
                pctLbl.setPrefWidth(110);
                pctLbl.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(15, nameLbl, track, pctLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                chart.getChildren().add(row);
                shown++;
            }

            if (shown == 0) {
                chart.getChildren().add(noDataLabel());
            }

        } catch (SQLDataException e) {
            chart.getChildren().add(noDataLabel());
        }

        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monthly Inscriptions Bar Chart (vertical)
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildMonthlyChart() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(cardStyle());

        Map<String, Integer> monthData = joinedService.getRegistrationsByMonth();

        if (monthData.isEmpty()) {
            card.getChildren().add(noDataLabel());
            return card;
        }

        int maxVal = monthData.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxVal == 0) maxVal = 1;
        final int maxHeight = 140;

        HBox bars = new HBox(10);
        bars.setAlignment(Pos.BOTTOM_RIGHT);
        bars.setPrefHeight(maxHeight + 30);

        for (Map.Entry<String, Integer> entry : monthData.entrySet()) {
            String month = entry.getKey();
            int count = entry.getValue();
            double barH = (double) count / maxVal * maxHeight;

            VBox barGroup = new VBox(4);
            barGroup.setAlignment(Pos.BOTTOM_CENTER);

            Label cntLbl = new Label(String.valueOf(count));
            cntLbl.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11; -fx-font-weight: bold;");

            Rectangle bar = new Rectangle(30, Math.max(4, barH));
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bar.setFill(Color.web("#10b981", 0.8));

            String shortMonth = month.length() >= 7 ? month.substring(5) : month;
            Label mLbl = new Label(shortMonth);
            mLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10;");

            barGroup.getChildren().addAll(cntLbl, bar, mLbl);
            bars.getChildren().add(barGroup);
        }

        card.getChildren().add(bars);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rating Distribution Chart
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildRatingChart() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(cardStyle());

        Map<Integer, Integer> dist = eventService.getRatingDistribution();
        int total = dist.values().stream().mapToInt(Integer::intValue).sum();

        String[] stars = {"⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"};
        String[] colors = {"#ef4444", "#f97316", "#f59e0b", "#84cc16", "#10b981"};

        for (int i = 1; i <= 5; i++) {
            int count = dist.getOrDefault(i, 0);
            double pct = total > 0 ? (double) count / total : 0;

            Label starLbl = new Label(stars[i - 1]);
            starLbl.setStyle("-fx-font-size: 13;");
            starLbl.setPrefWidth(90);

            StackPane track = new StackPane();
            track.setPrefHeight(22);
            track.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");
            HBox.setHgrow(track, Priority.ALWAYS);

            Rectangle fill = new Rectangle();
            fill.setHeight(22);
            fill.setArcWidth(5);
            fill.setArcHeight(5);
            fill.setFill(Color.web(colors[i - 1], 0.8));
            final double p = pct;
            track.widthProperty().addListener((obs, ov, nv) -> fill.setWidth(nv.doubleValue() * p));

            StackPane fillPane = new StackPane(fill);
            fillPane.setAlignment(Pos.CENTER_LEFT);
            track.getChildren().add(fillPane);

            Label countLbl = new Label(String.valueOf(count));
            countLbl.setStyle("-fx-text-fill: " + colors[i - 1] + "; -fx-font-size: 12; -fx-font-weight: bold;");
            countLbl.setPrefWidth(40);
            countLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, starLbl, track, countLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(row);
        }

        if (total == 0) {
            card.getChildren().add(noDataLabel());
        }

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String cardStyle() {
        return "-fx-background-color: rgba(255,255,255,0.04); " +
               "-fx-background-radius: 16; -fx-border-radius: 16; " +
               "-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1;";
    }

    private Label noDataLabel() {
        Label l = new Label("Aucune donnée disponible.");
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-style: italic;");
        return l;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
