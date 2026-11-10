package com.sms.view;

import com.sms.Main;
import com.sms.dao.AttendanceDAO;
import com.sms.dao.StudentDAO;
import com.sms.model.Student;
import com.sms.model.User;
import com.sms.util.AccessControl;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AttendanceView {
    private final Stage stage; private final User user;
    private final BorderPane root=new BorderPane();
    private final AttendanceDAO attendance=new AttendanceDAO();
    private final StudentDAO students=new StudentDAO();
    private final ComboBox<Student> studentBox=new ComboBox<>();
    private final DatePicker date=new DatePicker(LocalDate.now());
    private final ComboBox<String> status=new ComboBox<>();
    private final TableView<com.sms.model.AttendanceRow> daily=new TableView<>();
    private final TableView<com.sms.model.AttendanceSummary> monthly=new TableView<>();

    public AttendanceView(Stage stage,User user){this.stage=stage;this.user=user;build();loadStudents();loadDaily();}
    public Parent getRoot(){return root;}

    private void build(){
        root.getStyleClass().add("app-root");root.setLeft(sidebar());
        VBox main=new VBox(14);main.setPadding(new Insets(26));main.getStyleClass().add("main-content");
        Label t=new Label("Attendance");t.getStyleClass().add("page-title");
        Label sub=new Label("Daily marking + monthly attendance summary and low-attendance alerts");sub.getStyleClass().add("page-subtitle");

        studentBox.setPrefWidth(300);studentBox.setPromptText("Select student");
        status.setItems(FXCollections.observableArrayList("PRESENT","ABSENT"));status.setValue("PRESENT");
        Button save=new Button("Save Attendance");save.getStyleClass().add("primary-button");save.setOnAction(e->save());
        save.setDisable(!AccessControl.canManageAcademicRecords(user));
        Button load=new Button("Refresh");load.getStyleClass().add("secondary-button");load.setOnAction(e->loadDaily());

        if (AccessControl.isStudent(user)) { studentBox.setDisable(true); status.setDisable(true); }
        date.setOnAction(e -> loadDaily());
        HBox mark=new HBox(10,studentBox,date,status,save,load);mark.getStyleClass().add("toolbar-card");

        daily.getColumns().setAll(
                c("Student ID",r->r.studentId(),120),
                c("Name",r->r.studentName(),220),
                c("Date",r->r.date(),130),
                c("Status",r->r.status(),110)
        );
        daily.setPlaceholder(new Label("No attendance marked for this date."));

        monthly.getColumns().setAll(
                cs("Student ID",r->r.studentId(),120),
                cs("Name",r->r.studentName(),190),
                cs("Total Days",r->""+r.totalDays(),90),
                cs("Present",r->""+r.presentDays(),80),
                cs("Attendance",r->String.format("%.1f%%",r.percentage()),100)
        );
        monthly.setPlaceholder(new Label("No monthly data yet."));

        Label dtitle=new Label("Daily Records");dtitle.getStyleClass().add("panel-title");
        Label mtitle=new Label("Monthly Summary");mtitle.getStyleClass().add("panel-title");

        VBox p1=new VBox(8,dtitle,daily);p1.getStyleClass().add("panel");VBox.setVgrow(daily,Priority.ALWAYS);
        VBox p2=new VBox(8,mtitle,monthly);p2.getStyleClass().add("panel");VBox.setVgrow(monthly,Priority.ALWAYS);

        main.getChildren().addAll(t,sub,mark,p1,p2);
        VBox.setVgrow(p1,Priority.ALWAYS);VBox.setVgrow(p2,Priority.ALWAYS);
        root.setCenter(main);
    }

    private VBox sidebar(){
        VBox s=new VBox(8);s.setPrefWidth(240);s.getStyleClass().add("sidebar");s.setPadding(new Insets(26,15,20,15));
        Label l=new Label("SMS");l.getStyleClass().add("sidebar-logo");Label sub=new Label("Student Management");sub.getStyleClass().add("sidebar-subtitle");
        s.getChildren().addAll(new VBox(2,l,sub),new Separator());
        s.getChildren().add(nav("▣   Dashboard",()->back(new DashboardView(stage,user).getRoot())));
        if(AccessControl.canManageStudents(user))s.getChildren().add(nav("◉   Students",()->back(new StudentView(stage,user).getRoot())));
        s.getChildren().add(nav("◷   Attendance",null));
        s.getChildren().add(nav("★   Results",()->back(new ResultView(stage,user).getRoot())));
        s.getChildren().add(nav("◎   My Profile",()->back(new ProfileView(stage,user).getRoot())));
        return s;
    }
    private Button nav(String t,Runnable r){Button b=new Button(t);b.setMaxWidth(Double.MAX_VALUE);b.getStyleClass().add(t.contains("Attendance")?"nav-active":"nav-button");if(r!=null)b.setOnAction(e->r.run());return b;}
    private void back(Parent p){stage.setScene(new SceneWithCss(p).scene());}
    private void loadStudents(){var list=AccessControl.isStudent(user) && user.getStudentId()!=null ? java.util.List.of(students.byDbId(user.getStudentId())) : students.findAll();
        list = list.stream().filter(java.util.Objects::nonNull).toList(); studentBox.setItems(FXCollections.observableArrayList(list));studentBox.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getStudentId() + " — " + item.getFullName());
                }
            });
            studentBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getStudentId() + " — " + item.getFullName());
                }
            });
        if (AccessControl.isStudent(user) && !list.isEmpty()) studentBox.setValue(list.getFirst());
    }
    private void save(){try{if(!AccessControl.canManageAcademicRecords(user))throw new IllegalStateException("Your account cannot change attendance.");if(studentBox.getValue()==null)throw new IllegalArgumentException("Select a student.");attendance.save(studentBox.getValue().getId(),date.getValue().toString(),status.getValue());loadDaily();}catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
    private void loadDaily(){try{String selectedDate=date.getValue().toString(); if(AccessControl.isStudent(user)) { if(user.getStudentId()==null) throw new IllegalStateException("No student profile is linked to this account."); daily.setItems(FXCollections.observableArrayList(attendance.dailyForStudent(user.getStudentId(),selectedDate))); var summary=attendance.summaryForStudent(user.getStudentId()); monthly.setItems(FXCollections.observableArrayList(summary==null?java.util.List.of():java.util.List.of(summary))); } else {daily.setItems(FXCollections.observableArrayList(attendance.daily(selectedDate)));monthly.setItems(FXCollections.observableArrayList(attendance.monthly(selectedDate.substring(0,7))));}}catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
    private <T> TableColumn<T,String> c(String h,java.util.function.Function<T,String> fn,double w){TableColumn<T,String> c=new TableColumn<>(h);c.setPrefWidth(w);c.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));return c;}
    private <T> TableColumn<T,String> cs(String h,java.util.function.Function<T,String> fn,double w){return c(h,fn,w);}
}
