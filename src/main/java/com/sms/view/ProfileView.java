package com.sms.view;

import com.sms.dao.ResultDAO;
import com.sms.dao.StudentDAO;
import com.sms.model.Student;
import com.sms.model.User;
import com.sms.util.PdfGenerator;
import com.sms.util.AccessControl;
import com.sms.util.UiTasks;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ProfileView {
    private final Stage stage; private final User user; private final Student selected;
    private final BorderPane root=new BorderPane();
    private final StudentDAO students=new StudentDAO();

    public ProfileView(Stage stage,User user){this(stage,user,user.getStudentId()==null?null:new StudentDAO().byDbId(user.getStudentId()));}
    public ProfileView(Stage stage,User user,Student selected){this.stage=stage;this.user=user;this.selected=selected;build();}
    public Parent getRoot(){return root;}

    private void build(){
        root.getStyleClass().add("app-root");
        if(selected==null){
            VBox empty=new VBox(10,new Label("No student profile linked to this account."),new Label("Login as a student demo account or choose a student from the Students module."));
            empty.setAlignment(Pos.CENTER);empty.getStyleClass().add("main-content");root.setCenter(empty);return;
        }
        if (!AccessControl.canViewStudent(user, selected.getId())) {
            VBox denied = new VBox(new Label("You do not have permission to view this student profile."));
            denied.setAlignment(Pos.CENTER); denied.getStyleClass().add("main-content"); root.setCenter(denied); return;
        }
        if(user.getRole().equals("STUDENT")){
            root.setCenter(profileContent());
        }else{
            root.setLeft(sidebar());
            root.setCenter(profileContent());
        }
    }

    private VBox profileContent(){
        VBox main=new VBox(16);main.setPadding(new Insets(26));main.getStyleClass().add("main-content");
        HBox header=new HBox(15);
        VBox titles=new VBox(3);
        Label title=new Label("Student Profile");title.getStyleClass().add("page-title");
        Label sub=new Label("Unified personal, guardian and academic record");sub.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title,sub);Region g=new Region();HBox.setHgrow(g,Priority.ALWAYS);

        Button id=new Button("Generate ID Card");id.getStyleClass().add("primary-button");id.setOnAction(e->idCard());
        header.getChildren().addAll(titles,g,id);

        HBox identity=new HBox(18);
        VBox card=new VBox(7);card.getStyleClass().add("profile-hero");
        Label name=new Label(selected.getFullName());name.getStyleClass().add("profile-name");
        Label sid=new Label(selected.getStudentId());sid.getStyleClass().add("profile-id");
        Label meta=new Label(selected.getDepartment()+"  •  "+selected.getCourse()+"  •  Year "+selected.getYear()+"  •  Semester "+selected.getSemester());
        meta.getStyleClass().add("profile-meta");
        card.getChildren().addAll(name,sid,meta);

        Region av=avatar();
        identity.getChildren().addAll(av,card);

        GridPane details=new GridPane();details.setHgap(15);details.setVgap(12);details.getStyleClass().add("panel");
        add(details,0,0,"Email",selected.getEmail());add(details,2,0,"Phone",selected.getPhone());
        add(details,0,1,"DOB",dash(selected.getDob()));add(details,2,1,"Address",dash(selected.getAddress()));
        add(details,0,2,"Guardian",dash(selected.getGuardianName()));add(details,2,2,"Guardian Phone",dash(selected.getGuardianPhone()));

        HBox stats=new HBox(13,stat("MARKS",String.format("%.1f%%",selected.getMarks())),
                stat("ATTENDANCE",String.format("%.1f%%",selected.getAttendance())),
                stat("GRADE",selected.getGrade()));
        main.getChildren().addAll(header,identity,details,stats);
        return main;
    }

    private Region avatar(){
        StackPane holder=new StackPane();holder.setPrefSize(115,115);holder.getStyleClass().add("avatar-box");
        try{
            if(selected.getPhotoPath()!=null&&!selected.getPhotoPath().isBlank()&&new File(selected.getPhotoPath()).exists()){
                ImageView img=new ImageView(new Image(new File(selected.getPhotoPath()).toURI().toString(),110,110,true,true));
                holder.getChildren().add(img);
            } else {
                Label l=new Label(initials(selected.getFullName()));l.getStyleClass().add("avatar-text");holder.getChildren().add(l);
            }
        }catch(Exception e){Label l=new Label("ST");l.getStyleClass().add("avatar-text");holder.getChildren().add(l);}
        return holder;
    }

    private VBox sidebar(){
        VBox s=new VBox(8);s.setPrefWidth(240);s.getStyleClass().add("sidebar");s.setPadding(new Insets(26,15,20,15));
        Label l=new Label("SMS");l.getStyleClass().add("sidebar-logo");Label sub=new Label("Student Management");sub.getStyleClass().add("sidebar-subtitle");
        s.getChildren().addAll(new VBox(2,l,sub),new Separator());
        s.getChildren().add(nav("▣   Dashboard",()->back(new DashboardView(stage,user).getRoot())));
        s.getChildren().add(nav("◉   Students",()->back(new StudentView(stage,user).getRoot())));
        s.getChildren().add(nav("◷   Attendance",()->back(new AttendanceView(stage,user).getRoot())));
        s.getChildren().add(nav("★   Results",()->back(new ResultView(stage,user).getRoot())));
        s.getChildren().add(nav("◎   My Profile",null)); return s;
    }
    private Button nav(String t,Runnable r){Button b=new Button(t);b.setMaxWidth(Double.MAX_VALUE);b.getStyleClass().add(t.contains("My Profile")?"nav-active":"nav-button");if(r!=null)b.setOnAction(e->r.run());return b;}
    private void back(Parent p){stage.setScene(new SceneWithCss(p).scene());}
    private VBox stat(String cap,String val){VBox b=new VBox(4);b.getStyleClass().add("stat-card");Label c=new Label(cap);c.getStyleClass().add("stat-caption");Label v=new Label(val);v.getStyleClass().add("stat-value");b.getChildren().addAll(c,v);return b;}
    private void add(GridPane g,int col,int row,String label,String value){Label l=new Label(label+"\n"+dash(value));l.getStyleClass().add("profile-field");g.add(l,col,row);}
    private String dash(String s){return s==null||s.isBlank()?"—":s;}
    private String initials(String n){if(n==null||n.isBlank())return"ST";String[] p=n.trim().split("\\s+");return p.length==1?p[0].substring(0,1).toUpperCase():(p[0].substring(0,1)+p[1].substring(0,1)).toUpperCase();}
    private void idCard(){FileChooser fc=new FileChooser();fc.setInitialFileName(selected.getStudentId()+"_id_card.pdf");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));File f=fc.showSaveDialog(stage);if(f!=null){UiTasks.run("id-card",()->{PdfGenerator.idCard(selected,f);return f;},file->new Alert(Alert.AlertType.INFORMATION,"ID card saved to: "+file.getAbsolutePath()).showAndWait(),error->new Alert(Alert.AlertType.ERROR,"Could not generate ID card. Please try again.").showAndWait());}}
}
