# Student Information App (Java / Spring Boot + MySQL)

## 1. Database (local)
Install MySQL 8, make sure it is running on localhost:3306. The app creates the
`student_db` database and its tables automatically on first start.

## 2. Run
Needs Java 17+ and Maven.

Windows (PowerShell):
    $env:DB_USER="root"; $env:DB_PASSWORD="your-mysql-password"
    mvn spring-boot:run

Mac/Linux:
    DB_USER=root DB_PASSWORD=your-mysql-password mvn spring-boot:run

Open http://localhost:8080 and sign in with **admin / admin123** (change later).

## 3. Move to AWS RDS later
Create a MySQL RDS instance, allow your app's IP/security group on port 3306, then set:
    DB_URL=jdbc:mysql://<rds-endpoint>:3306/student_db?createDatabaseIfNotExist=true&useSSL=true
    DB_USER=<rds user>   DB_PASSWORD=<rds password>
No code changes needed.

## Banner images
Drop your own `login.jpg`, `students.jpg`, `teachers.jpg` into `src/main/resources/static/img/`.
They automatically replace the built-in placeholder banners.
