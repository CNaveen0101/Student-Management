package com.school;

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Models {

    @Entity(name = "Student") @Table(name = "students")
    public static class Student {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(unique = true) public String rollNo;
        @Column(nullable = false) public String name;
        public String className, department, email, phone, dob;
        public String guardianName, guardianMobile, address;
        public LocalDateTime createdAt = LocalDateTime.now();
    }

    @Entity(name = "Teacher") @Table(name = "teachers")
    public static class Teacher {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(unique = true) public String teacherId;
        @Column(nullable = false) public String name;
        public String department, subject, email, phone, qualification, hireDate;
        public Integer experienceYears;
        @Column(length = 1000) public String experienceDetails;
        public LocalDateTime createdAt = LocalDateTime.now();
    }

    @Entity(name = "AppUser") @Table(name = "app_users")
    public static class AppUser {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        @Column(unique = true, nullable = false) public String username;
        @Column(nullable = false) public String passwordHash;
    }

    public interface StudentRepo extends JpaRepository<Student, Long> {
        List<Student> findTop5ByOrderByIdDesc();
        @Query("select coalesce(s.department,'Unassigned'), count(s) from Student s group by s.department")
        List<Object[]> byDepartment();
    }
    public interface TeacherRepo extends JpaRepository<Teacher, Long> {
        List<Teacher> findTop5ByOrderByIdDesc();
    }
    public interface UserRepo extends JpaRepository<AppUser, Long> {
        Optional<AppUser> findByUsername(String username);
    }
}
