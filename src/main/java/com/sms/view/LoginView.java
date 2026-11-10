package com.sms.view;

import com.sms.Main;
import com.sms.dao.UserDAO;
import com.sms.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LoginView {
    private final BorderPane root = new BorderPane();

    public LoginView(Stage stage) {
        root.getStyleClass().add("login-root");

        VBox brand = new VBox(20);
        brand.getStyleClass().add("login-brand");
        brand.setPadding(new Insets(55));
        brand.setPrefWidth(560);

        Label mark = new Label("SMS");
        mark.getStyleClass().add("brand-mark");

        Label title = new Label("Smart Student\nManagement System");
        title.getStyleClass().add("brand-title");

        Label desc = new Label("A complete academic management desktop application for student profiles, attendance, results and reports.");
        desc.setWrapText(true);
        desc.setMaxWidth(420);
        desc.getStyleClass().add("brand-desc");

        VBox points = new VBox(12,
                feature("✓", "Student profiles & guardian details"),
                feature("✓", "Daily/monthly attendance tracking"),
                feature("✓", "Subject-wise result & gradebook"),
                feature("✓", "PDF reports & student ID cards")
        );
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label tech = new Label("JAVA 21   •   JAVAFX   •   JDBC   •   SQLITE");
        tech.getStyleClass().add("brand-tech");

        brand.getChildren().addAll(mark, title, desc, points, spacer, tech);

        VBox form = new VBox(18);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(40));
        form.getStyleClass().add("login-form");

        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.getStyleClass().add("login-card");

        Label heading = new Label("Welcome back");
        heading.getStyleClass().add("login-heading");

        Label sub = new Label("Sign in with your role account");
        sub.getStyleClass().add("login-subheading");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.getStyleClass().add("modern-field");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.getStyleClass().add("modern-field");

        Label message = new Label();
        message.getStyleClass().add("error-label");
        message.setWrapText(true);

        Button login = new Button("Sign In  →");
        login.getStyleClass().add("login-button");
        login.setMaxWidth(Double.MAX_VALUE);

        VBox demo = new VBox(4,
                new Label("Demo accounts") {{
                    getStyleClass().add("demo-title");
                }},
                new Label("admin/admin123   •   teacher/teacher123   •   student/student123") {{
                    getStyleClass().add("demo-text");
                    setWrapText(true);
                }}
        );
        demo.getStyleClass().add("demo-box");

        login.setOnAction(e -> {
            if (username.getText().isBlank() || password.getText().isBlank()) {
                message.setText("Enter both username and password.");
            } else {
                try {
                    User user = new UserDAO().authenticate(username.getText().trim(), password.getText());
                    if (user == null) {
                        message.setText("Invalid username or password.");
                        return;
                    }
                    stage.setScene(new SceneWithCss(new DashboardView(stage, user).getRoot()).scene());
                } catch (RuntimeException ex) {
                    message.setText(ex.getMessage());
                }
            }
        });

        password.setOnAction(e -> login.fire());

        card.getChildren().addAll(heading, sub,
                labelled("Username", username),
                labelled("Password", password),
                message, login, demo);
        form.getChildren().add(card);

        root.setLeft(brand);
        root.setCenter(form);
    }

    private HBox feature(String icon, String text) {
        Label a = new Label(icon);
        a.getStyleClass().add("feature-icon");
        Label b = new Label(text);
        b.getStyleClass().add("feature-text");
        return new HBox(10, a, b);
    }

    private VBox labelled(String text, Control control) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return new VBox(7, l, control);
    }

    public Parent getRoot() { return root; }
}
