package com.sms.view;

import com.sms.Main;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class SceneWithCss {
    private final Parent root;
    public SceneWithCss(Parent root) { this.root = root; }
    public Scene scene() {
        Scene s = new Scene(root, Main.WIDTH, Main.HEIGHT);
        var stylesheet = Main.class.getResource("/css/style.css");
        if (stylesheet != null) s.getStylesheets().add(stylesheet.toExternalForm());
        return s;
    }
}
