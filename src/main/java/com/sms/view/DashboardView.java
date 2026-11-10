package com.sms.view;

import com.sms.Main;
import com.sms.dao.StudentDAO;
import com.sms.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DashboardView {
    private final BorderPane root = new BorderPane();
    private final Stage stage;
    private final User user;
    private final StudentDAO students = new StudentDAO();

    public DashboardView(Stage stage, User user) {
        this.stage = stage;
        this.user = user;
        build();
    }

    public Parent getRoot() { return root; }

    private void build() {
        root.getStyleClass().add("app-root");
        root.setLeft(sidebar());
        root.setCenter(main());
    }

    private VBox sidebar() {
        VBox side = new VBox(8);
        side.setPrefWidth(240);
        side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(26, 15, 20, 15));

        Label logo = new Label("SMS");
        logo.getStyleClass().add("sidebar-logo");
        Label title = new Label("Student Management");
        title.getStyleClass().add("sidebar-subtitle");

        Region r = new Region();
        VBox.setVgrow(r, Priority.NEVER);

        Button dash = nav("▣   Dashboard", true, () -> {});
        Button student = nav("◉   Students", false, () -> showStudents());
        Button attend = nav("◷   Attendance", false, () -> showAttendance());
        Button results = nav("★   Results", false, () -> showResults());
        Button profile = nav("◎   My Profile", false, () -> showProfile());

        if (user.getRole().equals("STUDENT")) {
            student.setDisable(true);
        }

        Button logout = nav("↪   Logout", false, () -> {
            LoginView login = new LoginView(stage);
            javafx.scene.Scene scene = new javafx.scene.Scene(login.getRoot(), Main.WIDTH, Main.HEIGHT);
            var stylesheet = Main.class.getResource("/css/style.css");
            if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
            stage.setScene(scene);
        });

        Region bottom = new Region();
        VBox.setVgrow(bottom, Priority.ALWAYS);

        side.getChildren().addAll(
                new VBox(2, logo, title),
                new Separator(),
                dash,
                student,
                attend,
                results,
                profile,
                bottom,
                new Label(user.getUsername() + " • " + user.getRole()),
                logout
        );
        side.getChildren().get(side.getChildren().size()-2).getStyleClass().add("sidebar-user");
        return side;
    }

    private Button nav(String text, boolean active, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add(active ? "nav-active" : "nav-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private VBox main() {
        if (com.sms.util.AccessControl.isStudent(user)) return studentMain();
        VBox main = new VBox(18);
        main.setPadding(new Insets(28));
        main.getStyleClass().add("main-content");

        HBox head = new HBox(15);
        VBox ht = new VBox(2);
        Label page = new Label("Dashboard");
        page.getStyleClass().add("page-title");
        Label sub = new Label("A quick overview of your student data");
        sub.getStyleClass().add("page-subtitle");
        ht.getChildren().addAll(page, sub);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Label role = new Label(user.getRole());
        role.getStyleClass().add("role-pill");
        head.getChildren().addAll(ht, grow, role);

        HBox stats = new HBox(14,
                stat("TOTAL STUDENTS", String.valueOf(students.count())),
                stat("MALE", String.valueOf(students.countGender("Male"))),
                stat("FEMALE", String.valueOf(students.countGender("Female"))),
                stat("LOW ATTENDANCE", String.valueOf(students.countLowAttendance()))
        );

        HBox row = new HBox(16);
        VBox chartPanel = new VBox(10);
        chartPanel.getStyleClass().add("panel");
        chartPanel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chartPanel, Priority.ALWAYS);
        Label chartTitle = new Label("Gender Distribution");
        chartTitle.getStyleClass().add("panel-title");
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        long male = students.countGender("Male");
        long female = students.countGender("Female");
        if (male > 0) chart.getData().add(new PieChart.Data("Male", male));
        if (female > 0) chart.getData().add(new PieChart.Data("Female", female));
        chartPanel.getChildren().addAll(chartTitle, chart);

        VBox alert = new VBox(10);
        alert.getStyleClass().add("panel");
        alert.setPrefWidth(420);
        Label alertTitle = new Label("Quick Actions");
        alertTitle.getStyleClass().add("panel-title");
        Button manage = primary("Manage Students", () -> showStudents());
        Button att = secondary("Mark Attendance", () -> showAttendance());
        Button res = secondary("Manage Results", () -> showResults());
        alert.getChildren().addAll(alertTitle, manage, att, res);

        row.getChildren().addAll(chartPanel, alert);

        HBox recentHeader = new HBox();
        Label recTitle = new Label("Recently Added Students");
        recTitle.getStyleClass().add("panel-title");
        recentHeader.getChildren().add(recTitle);

        VBox recent = new VBox(8);
        recent.getStyleClass().add("panel");
        students.recent(4).forEach(s -> {
            HBox item = new HBox(10);
            item.getStyleClass().add("recent-row");
            VBox info = new VBox(2,
                    new Label(s.getFullName()) {{
                        getStyleClass().add("recent-name");
                    }},
                    new Label(s.getStudentId()+" • "+s.getDepartment()+" • Grade "+s.getGrade()) {{
                        getStyleClass().add("recent-meta");
                    }});
            item.getChildren().add(info);
            recent.getChildren().add(item);
        });

        main.getChildren().addAll(head, stats, row, recentHeader, recent);
        return main;
    }

    /** Student accounts deliberately do not receive aggregate institution data. */
    private VBox studentMain() {
        VBox main = new VBox(18);
        main.setPadding(new Insets(28));
        main.getStyleClass().add("main-content");
        Label title = new Label("My Dashboard"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("View your profile, attendance, results, and reports."); subtitle.getStyleClass().add("page-subtitle");
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel");
        Label panelTitle = new Label("My Academic Record"); panelTitle.getStyleClass().add("panel-title");
        Button profile = primary("View My Profile", this::showProfile);
        Button attendance = secondary("View My Attendance", this::showAttendance);
        Button results = secondary("View My Results & Reports", this::showResults);
        panel.getChildren().addAll(panelTitle, profile, attendance, results);
        main.getChildren().addAll(title, subtitle, panel);
        return main;
    }

    private VBox stat(String caption, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("stat-card");
        HBox.setHgrow(box, Priority.ALWAYS);
        Label c = new Label(caption);
        c.getStyleClass().add("stat-caption");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        box.getChildren().addAll(c, v);
        return box;
    }

    private Button primary(String text, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("primary-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button secondary(String text, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("secondary-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void showStudents() {
        if (!com.sms.util.AccessControl.canManageStudents(user)) { showAccessDenied(); return; }
        setRoot(new StudentView(stage, user).getRoot());
    }
    private void showAttendance() { setRoot(new AttendanceView(stage, user).getRoot()); }
    private void showResults() { setRoot(new ResultView(stage, user).getRoot()); }
    private void showProfile() { setRoot(new ProfileView(stage, user).getRoot()); }

    private void setRoot(Parent view) {
        stage.setScene(new SceneWithCss(view).scene());
    }

    private void showAccessDenied() { new Alert(Alert.AlertType.WARNING, "Your account does not have access to student management.").showAndWait(); }
}
