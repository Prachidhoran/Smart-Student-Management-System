<<<<<<< HEAD
# Smart Student Management System — Pro Max

## Prerequisites

- JDK 21 (the project compiles with Java release 21)
- Apache Maven 3.9 or newer available on `PATH`
- Internet access once for Maven to download JavaFX, SQLite JDBC, and test dependencies

## Included
- Modern JavaFX dashboard
- Role-based login: Admin / Teacher / Student
- Full student profile with photo path + guardian details
- Student CRUD/search/filter
- Daily attendance marking
- Monthly attendance summary
- Below-75% dashboard alert
- Subject-wise gradebook
- Automatic percentage/grade calculation
- PDF report card generation
- PDF student ID card generation
- CSV export
- SQLite + JDBC persistence
- Demo data seeded automatically

## Demo accounts
Admin: `admin / admin123`  
Teacher: `teacher / teacher123`  
Student: `student / student123`

Demo passwords are automatically stored using PBKDF2 on first start. Existing databases that contain the original plaintext demo passwords are migrated in place; no reset is required.

## Run
Open the folder that contains `pom.xml` in IntelliJ IDEA.
Let Maven load dependencies.
Run: Maven -> Plugins -> javafx -> run

Or with Maven installed:
`mvn clean test`  
`mvn javafx:run`

## Fresh database
The app creates `student_management.db` automatically in the project working directory. Close the app before any reset. To reset demo data, delete only that file and run the application again; this permanently removes locally entered data.

## Troubleshooting

- **`mvn` is not recognized:** install Apache Maven, add its `bin` directory to `PATH`, then open a new terminal.
- **Wrong Java version:** run `java -version`; configure `JAVA_HOME` to a JDK 21 installation before running Maven.
- **JavaFX launch error:** run through `mvn javafx:run` rather than executing the class directly.
- **Database locked:** close any other copy of the application and retry. Each SQLite connection enables foreign keys and a five-second busy timeout.
=======
# Smart-Student-Management-System
>>>>>>> 21a57e0f5be819bf1532b89b09946fa738beea91
