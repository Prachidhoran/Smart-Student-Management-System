package com.sms.view;

import com.sms.dao.ResultDAO;
import com.sms.dao.StudentDAO;
import com.sms.model.ResultRow;
import com.sms.model.Student;
import com.sms.model.User;
import com.sms.util.PdfGenerator;
import com.sms.util.AccessControl;
import com.sms.dao.AttendanceDAO;
import com.sms.util.UiTasks;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class ResultView {
    private final Stage stage; private final User user;
    private final BorderPane root=new BorderPane();
    private final ResultDAO dao=new ResultDAO();
    private final StudentDAO students=new StudentDAO();
    private final ComboBox<Student> studentBox=new ComboBox<>();
    private final TextField subject=new TextField();
    private final TextField marks=new TextField();
    private final TextField maxMarks=new TextField("100");
    private final TableView<ResultRow> table=new TableView<>();
    private Student selected;

    public ResultView(Stage stage,User user){this.stage=stage;this.user=user;build();loadStudents();}
    public Parent getRoot(){return root;}

    private void build(){
        root.getStyleClass().add("app-root");root.setLeft(sidebar());
        VBox main=new VBox(14);main.setPadding(new Insets(26));main.getStyleClass().add("main-content");
        Label t=new Label("Results & Gradebook");t.getStyleClass().add("page-title");
        Label sub=new Label("Subject-wise marks, automatic grade calculation and report-card PDF");sub.getStyleClass().add("page-subtitle");

        studentBox.setPrefWidth(350);studentBox.setPromptText("Select student");
        studentBox.setOnAction(e->{selected=studentBox.getValue();load();});
        subject.setPromptText("Subject");subject.setPrefWidth(190);
        marks.setPromptText("Marks");marks.setPrefWidth(100);
        maxMarks.setPrefWidth(90);
        Button save=new Button("Save Result");save.getStyleClass().add("primary-button");save.setOnAction(e->save());
        save.setDisable(!AccessControl.canManageAcademicRecords(user));
        Button report=new Button("Generate Report Card");report.getStyleClass().add("secondary-button");report.setOnAction(e->report());
        HBox form=new HBox(9,studentBox,subject,marks,maxMarks,save,report);form.getStyleClass().add("toolbar-card");

        table.getColumns().setAll(
                col("Subject",r->r.subject(),220),
                col("Marks",r->String.format("%.1f",r.marks()),90),
                col("Max",r->String.format("%.1f",r.maxMarks()),90),
                col("Percentage",r->String.format("%.1f%%",r.percentage()),110),
                col("Grade",r->r.grade(),80)
        );
        table.setPlaceholder(new Label("Select a student to see results."));

        Button delete=new Button("Delete Selected Result");delete.getStyleClass().add("danger-button");delete.setOnAction(e->delete());delete.setDisable(!AccessControl.canManageAcademicRecords(user));
        VBox panel=new VBox(8,new Label("Subject Results"){{getStyleClass().add("panel-title");}},table,delete);panel.getStyleClass().add("panel");VBox.setVgrow(table,Priority.ALWAYS);

        main.getChildren().addAll(t,sub,form,panel);VBox.setVgrow(panel,Priority.ALWAYS);
        root.setCenter(main);
    }

    private VBox sidebar(){
        VBox s=new VBox(8);s.setPrefWidth(240);s.getStyleClass().add("sidebar");s.setPadding(new Insets(26,15,20,15));
        Label l=new Label("SMS");l.getStyleClass().add("sidebar-logo");Label sub=new Label("Student Management");sub.getStyleClass().add("sidebar-subtitle");
        s.getChildren().addAll(new VBox(2,l,sub),new Separator());
        s.getChildren().add(nav("▣   Dashboard",()->back(new DashboardView(stage,user).getRoot())));
        if (AccessControl.canManageStudents(user)) s.getChildren().add(nav("◉   Students",()->back(new StudentView(stage,user).getRoot())));
        s.getChildren().add(nav("◷   Attendance",()->back(new AttendanceView(stage,user).getRoot())));
        s.getChildren().add(nav("★   Results",null));s.getChildren().add(nav("◎   My Profile",()->back(new ProfileView(stage,user).getRoot())));
        return s;
    }
    private Button nav(String t,Runnable r){Button b=new Button(t);b.setMaxWidth(Double.MAX_VALUE);b.getStyleClass().add(t.contains("Results")?"nav-active":"nav-button");if(r!=null)b.setOnAction(e->r.run());return b;}
    private void back(Parent p){stage.setScene(new SceneWithCss(p).scene());}
    private void loadStudents(){var list=AccessControl.isStudent(user) && user.getStudentId()!=null ? java.util.List.of(students.byDbId(user.getStudentId())) : students.findAll(); list=list.stream().filter(java.util.Objects::nonNull).toList();studentBox.setItems(FXCollections.observableArrayList(list));studentBox.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getStudentId() + " — " + item.getFullName());
                }
            });
        if (AccessControl.isStudent(user) && !list.isEmpty()) { selected = list.getFirst(); studentBox.setValue(selected); load(); }
            studentBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getStudentId() + " — " + item.getFullName());
                }
            });
    }
    private void load(){if(selected!=null)table.setItems(FXCollections.observableArrayList(dao.byStudent(selected.getId())));}
    private void save(){try{if(!AccessControl.canManageAcademicRecords(user))throw new IllegalStateException("Your account cannot change results.");if(selected==null)throw new IllegalArgumentException("Select a student.");String sub=subject.getText().trim();double m=Double.parseDouble(marks.getText().trim());double max=Double.parseDouble(maxMarks.getText().trim());dao.save(selected.getId(),sub,m,max);subject.clear();marks.clear();load();}catch(Exception e){new Alert(Alert.AlertType.ERROR,e.getMessage()).showAndWait();}}
    private void delete(){if(!AccessControl.canManageAcademicRecords(user)){new Alert(Alert.AlertType.WARNING,"Your account cannot delete results.").showAndWait();return;}var r=table.getSelectionModel().getSelectedItem();if(r==null){new Alert(Alert.AlertType.WARNING,"Select a result.").showAndWait();return;}if(new Alert(Alert.AlertType.CONFIRMATION,"Delete the selected result?",ButtonType.CANCEL,ButtonType.OK).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){dao.delete(r.id());load();}}
    private void report(){if(selected==null){new Alert(Alert.AlertType.WARNING,"Select a student.").showAndWait();return;}FileChooser fc=new FileChooser();fc.setInitialFileName(selected.getStudentId()+"_report_card.pdf");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));File f=fc.showSaveDialog(stage);if(f!=null){Student reportStudent=selected;UiTasks.run("report-card",()->{PdfGenerator.studentReport(reportStudent,dao.byStudent(reportStudent.getId()),new AttendanceDAO().summaryForStudent(reportStudent.getId()),f);return f;},file->new Alert(Alert.AlertType.INFORMATION,"Report card saved to: "+file.getAbsolutePath()).showAndWait(),error->new Alert(Alert.AlertType.ERROR,"Could not generate report card. Please try again.").showAndWait());}}
    private TableColumn<ResultRow,String> col(String h,java.util.function.Function<ResultRow,String> fn,double w){TableColumn<ResultRow,String> c=new TableColumn<>(h);c.setPrefWidth(w);c.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));return c;}
}
