package com.sms.view;

import com.sms.Main;
import com.sms.dao.StudentDAO;
import com.sms.model.Student;
import com.sms.model.User;
import com.sms.util.AccessControl;
import com.sms.util.ValidationUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.Optional;

public class StudentView {
    private final Stage stage;
    private final User user;
    private final BorderPane root = new BorderPane();
    private final StudentDAO dao = new StudentDAO();
    private final TableView<Student> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> department = new ComboBox<>();
    private final Label status = new Label();

    public StudentView(Stage stage, User user) {
        this.stage = stage; this.user = user;
        build();
        load();
    }

    public Parent getRoot(){ return root; }

    private void build() {
        root.getStyleClass().add("app-root");
        root.setLeft(sidebar());
        VBox main = new VBox(14);
        main.setPadding(new Insets(26));
        main.getStyleClass().add("main-content");

        HBox head = new HBox(10);
        VBox hv = new VBox(2);
        Label t = new Label("Student Management"); t.getStyleClass().add("page-title");
        Label st = new Label("Profiles, guardian details and academic information"); st.getStyleClass().add("page-subtitle");
        hv.getChildren().addAll(t,st);
        Region g=new Region(); HBox.setHgrow(g,Priority.ALWAYS);
        status.getStyleClass().add("role-pill");
        head.getChildren().addAll(hv,g,status);

        HBox tools = new HBox(10);
        search.setPromptText("Search ID, name, email or course...");
        search.getStyleClass().add("modern-field");
        HBox.setHgrow(search,Priority.ALWAYS);
        department.setItems(FXCollections.observableArrayList("All","CSE","ECE","ME","Civil","Electrical"));
        department.setValue("All"); department.getStyleClass().add("modern-field");
        Button sb=primary("Search",this::search);
        Button clear=secondary("Clear",this::load);
        tools.getChildren().addAll(search,department,sb,clear);

        HBox actions = new HBox(9);
        if(AccessControl.canManageStudents(user)) {
            actions.getChildren().add(primary("+ Add Student", ()->dialog(null)));
            actions.getChildren().add(secondary("Edit", this::edit));
            actions.getChildren().add(danger("Delete", this::delete));
        }
        actions.getChildren().add(secondary("View Profile", this::profile));
        if (AccessControl.canManageStudents(user)) actions.getChildren().add(secondary("Export CSV", this::export));

        setupTable();
        VBox panel=new VBox(table); panel.getStyleClass().add("panel"); VBox.setVgrow(table,Priority.ALWAYS);

        main.getChildren().addAll(head,tools,actions,panel);
        root.setCenter(main);
    }

    private VBox sidebar() {
        VBox side = new VBox(8);
        side.setPrefWidth(240); side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(26,15,20,15));
        Label logo=new Label("SMS"); logo.getStyleClass().add("sidebar-logo");
        Label sub=new Label("Student Management"); sub.getStyleClass().add("sidebar-subtitle");
        side.getChildren().addAll(new VBox(2,logo,sub), new Separator());
        side.getChildren().add(nav("▣   Dashboard", () -> back(new DashboardView(stage,user).getRoot())));
        side.getChildren().add(nav("◉   Students", null));
        side.getChildren().add(nav("◷   Attendance", () -> back(new AttendanceView(stage,user).getRoot())));
        side.getChildren().add(nav("★   Results", () -> back(new ResultView(stage,user).getRoot())));
        side.getChildren().add(nav("◎   My Profile", () -> back(new ProfileView(stage,user).getRoot())));
        Region r=new Region(); VBox.setVgrow(r,Priority.ALWAYS); side.getChildren().add(r);
        side.getChildren().add(new Label(user.getUsername()+" • "+user.getRole()));
        return side;
    }

    private Button nav(String text,Runnable action){
        Button b=new Button(text); b.setMaxWidth(Double.MAX_VALUE); b.getStyleClass().add(text.contains("Students")?"nav-active":"nav-button");
        if(action!=null)b.setOnAction(e->action.run()); return b;
    }
    private void back(Parent p){stage.setScene(new SceneWithCss(p).scene());}

    private void setupTable(){
        TableColumn<Student,String> c1=col("ID",s->s.getStudentId(),120);
        TableColumn<Student,String> c2=col("Name",s->s.getFullName(),190);
        TableColumn<Student,String> c3=col("Dept",s->s.getDepartment(),100);
        TableColumn<Student,String> c4=col("Course",s->s.getCourse(),90);
        TableColumn<Student,String> c5=col("Year",s->"Year "+s.getYear(),70);
        TableColumn<Student,String> c6=col("Marks",s->String.format("%.1f",s.getMarks()),75);
        TableColumn<Student,String> c7=col("Attendance",s->String.format("%.1f%%",s.getAttendance()),95);
        TableColumn<Student,String> c8=col("Grade",Student::getGrade,70);
        table.getColumns().setAll(c1,c2,c3,c4,c5,c6,c7,c8);
        table.setPlaceholder(new Label("No students found. Add the first record."));
    }

    private <T> TableColumn<Student,String> col(String title,java.util.function.Function<Student,String> fn,double w){
        TableColumn<Student,String> c=new TableColumn<>(title); c.setPrefWidth(w); c.setCellValueFactory(d->new SimpleStringProperty(fn.apply(d.getValue()))); return c;
    }

    private void load(){
        try{var list=AccessControl.isStudent(user) && user.getStudentId()!=null ? java.util.List.of(dao.byDbId(user.getStudentId())) : dao.findAll();
            list = list.stream().filter(java.util.Objects::nonNull).toList(); table.setItems(FXCollections.observableArrayList(list));status.setText(list.size()+" student(s)");}
        catch(Exception e){status.setText("Database error"); alert(Alert.AlertType.ERROR,e.getMessage());}
    }
    private void search(){
        try{if (AccessControl.isStudent(user)) { load(); return; } var list=dao.search(search.getText(),department.getValue());table.setItems(FXCollections.observableArrayList(list));status.setText(list.size()+" found");}
        catch(Exception e){alert(Alert.AlertType.ERROR,e.getMessage());}
    }
    private void profile(){
        Student s=table.getSelectionModel().getSelectedItem(); if(s==null){alert(Alert.AlertType.WARNING,"Select a student.");return;}
        Stage dialog=new Stage(); dialog.initOwner(stage); dialog.setTitle("Student Profile");
        dialog.setScene(new SceneWithCss(new ProfileView(dialog,user,s).getRoot()).scene()); dialog.showAndWait();
    }
    private void edit(){Student s=table.getSelectionModel().getSelectedItem();if(s==null){alert(Alert.AlertType.WARNING,"Select a student.");return;}dialog(s);}
    private void delete(){
        Student s=table.getSelectionModel().getSelectedItem(); if(s==null){alert(Alert.AlertType.WARNING,"Select a student.");return;}
        if(new Alert(Alert.AlertType.CONFIRMATION,"Delete "+s.getFullName()+"?",ButtonType.CANCEL,ButtonType.OK).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){
            dao.delete(s.getId()); load();
        }
    }
    private void export(){
        FileChooser fc=new FileChooser();fc.setInitialFileName("students.csv");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File f=fc.showSaveDialog(stage); if(f!=null)dao.exportCsv(dao.findAll(),f);
    }

    private void dialog(Student existing){
        Dialog<ButtonType> d=new Dialog<>(); d.setTitle(existing==null?"Add Student":"Edit Student");
        ButtonType save=new ButtonType(existing==null?"Save":"Update",ButtonBar.ButtonData.OK_DONE); d.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(10);g.setPadding(new Insets(15));
        TextField id=f("Student ID");TextField name=f("Full Name");ComboBox<String> gender=cb("Male","Female","Other");
        TextField dob=f("YYYY-MM-DD");TextField email=f("Email");TextField phone=f("10-digit phone");TextField address=f("Address");
        ComboBox<String> dep=cb("CSE","ECE","ME","Civil","Electrical");ComboBox<String> course=cb("B.E.","B.Tech");
        ComboBox<Integer> year=new ComboBox<>(FXCollections.observableArrayList(1,2,3,4));ComboBox<Integer> sem=new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8));
        TextField marks=f("0-100");TextField att=f("0-100");TextField guardian=f("Guardian name");TextField gphone=f("Guardian phone");
        TextField photo=f("Optional image path"); Button browse=new Button("Choose Photo");
        browse.setOnAction(e->{FileChooser fc=new FileChooser();File file=fc.showOpenDialog(d.getOwner());if(file!=null)photo.setText(file.getAbsolutePath());});
        if(existing!=null){
            id.setText(existing.getStudentId());name.setText(existing.getFullName());gender.setValue(existing.getGender());dob.setText(existing.getDob());
            email.setText(existing.getEmail());phone.setText(existing.getPhone());address.setText(existing.getAddress());dep.setValue(existing.getDepartment());
            course.setValue(existing.getCourse());year.setValue(existing.getYear());sem.setValue(existing.getSemester());marks.setText(""+existing.getMarks());att.setText(""+existing.getAttendance());
            guardian.setText(existing.getGuardianName());gphone.setText(existing.getGuardianPhone());photo.setText(existing.getPhotoPath());
        } else {year.setValue(1);sem.setValue(1);}
        int r=0; add(g,r++,"Student ID *",id);add(g,r++,"Full Name *",name);add(g,r++,"Gender *",gender);add(g,r++,"DOB",dob);add(g,r++,"Email *",email);add(g,r++,"Phone *",phone);
        add(g,r++,"Address",address);add(g,r++,"Department *",dep);add(g,r++,"Course *",course);add(g,r++,"Year",year);add(g,r++,"Semester",sem);add(g,r++,"Marks",marks);add(g,r++,"Attendance",att);add(g,r++,"Guardian Name",guardian);add(g,r++,"Guardian Phone",gphone);
        HBox ph=new HBox(8,photo,browse);HBox.setHgrow(photo,Priority.ALWAYS);add(g,r,"Photo",ph);
        d.getDialogPane().setContent(new ScrollPane(g));
        Button saveNode=(Button)d.getDialogPane().lookupButton(save);
        saveNode.addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            try{
                Student s=existing==null?new Student():existing;
                s.setStudentId(id.getText().trim());s.setFullName(name.getText().trim());s.setGender(gender.getValue());s.setDob(dob.getText().trim());
                s.setEmail(email.getText().trim());s.setPhone(phone.getText().trim());s.setAddress(address.getText().trim());s.setDepartment(dep.getValue());
                s.setCourse(course.getValue());s.setYear(year.getValue());s.setSemester(sem.getValue());s.setMarks(Double.parseDouble(marks.getText().trim()));
                s.setAttendance(Double.parseDouble(att.getText().trim()));s.setGuardianName(guardian.getText().trim());s.setGuardianPhone(gphone.getText().trim());s.setPhotoPath(photo.getText().trim());
                validate(s,existing==null?null:existing.getId());
                if(existing==null)dao.insert(s);else dao.update(s);
            }catch(Exception ex){e.consume();alert(Alert.AlertType.ERROR,ex.getMessage());}
        });
        if(d.showAndWait().orElse(ButtonType.CANCEL)==save)load();
    }

    private void validate(Student s,Integer ex){
        ValidationUtil.validateStudent(s);
        if(dao.existsStudentId(s.getStudentId(),ex))throw new IllegalArgumentException("Student ID already exists.");
    }

    private TextField f(String p){TextField t=new TextField();t.setPromptText(p);t.setPrefWidth(330);t.getStyleClass().add("modern-field");return t;}
    @SafeVarargs private final <T> ComboBox<T> cb(T... items){ComboBox<T> c=new ComboBox<>(FXCollections.observableArrayList(items));c.setPrefWidth(330);c.setPromptText("Select");c.getStyleClass().add("modern-field");return c;}
    private void add(GridPane g,int row,String label,javafx.scene.Node n){g.add(new Label(label),0,row);g.add(n,1,row);}
    private Button primary(String s,Runnable r){Button b=new Button(s);b.getStyleClass().add("primary-button");b.setOnAction(e->r.run());return b;}
    private Button secondary(String s,Runnable r){Button b=new Button(s);b.getStyleClass().add("secondary-button");b.setOnAction(e->r.run());return b;}
    private Button danger(String s,Runnable r){Button b=new Button(s);b.getStyleClass().add("danger-button");b.setOnAction(e->r.run());return b;}
    private void alert(Alert.AlertType type,String msg){new Alert(type,msg).showAndWait();}
}
