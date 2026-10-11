package com.school;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class Api {
    static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();
    static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    private final Models.StudentRepo students;
    private final Models.TeacherRepo teachers;
    private final Models.UserRepo users;

    public Api(Models.StudentRepo s, Models.TeacherRepo t, Models.UserRepo u) {
        this.students = s; this.teachers = t; this.users = u;
    }

    // ---------- Login ----------
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        Models.AppUser u = users.findByUsername(body.getOrDefault("username", "")).orElse(null);
        if (u == null || !ENC.matches(body.getOrDefault("password", ""), u.passwordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong username or password.");
        String token = UUID.randomUUID().toString();
        TOKENS.put(token, u.username);
        return Map.of("token", token, "username", u.username);
    }

    // ---------- Students ----------
    @GetMapping("/students")
    public List<Models.Student> listStudents() { return students.findAll(Sort.by("name")); }

    @PostMapping("/students")
    public Models.Student addStudent(@RequestBody Models.Student s) { s.id = null; return saveStudent(s); }

    @PutMapping("/students/{id}")
    public Models.Student updateStudent(@PathVariable Long id, @RequestBody Models.Student s) {
        Models.Student old = students.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));
        s.id = id; s.createdAt = old.createdAt;
        return saveStudent(s);
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) { students.deleteById(id); return ResponseEntity.noContent().build(); }

    private Models.Student saveStudent(Models.Student s) {
        if (blank(s.name)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        boolean auto = blank(s.rollNo);
        if (auto) s.rollNo = null;
        Models.Student saved = students.save(s);
        if (auto) { saved.rollNo = String.format("STU-%04d", saved.id); saved = students.save(saved); }
        return saved;
    }

    // ---------- Teachers ----------
    @GetMapping("/teachers")
    public List<Models.Teacher> listTeachers() { return teachers.findAll(Sort.by("name")); }

    @PostMapping("/teachers")
    public Models.Teacher addTeacher(@RequestBody Models.Teacher t) { t.id = null; return saveTeacher(t); }

    @PutMapping("/teachers/{id}")
    public Models.Teacher updateTeacher(@PathVariable Long id, @RequestBody Models.Teacher t) {
        Models.Teacher old = teachers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found."));
        t.id = id; t.createdAt = old.createdAt;
        return saveTeacher(t);
    }

    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) { teachers.deleteById(id); return ResponseEntity.noContent().build(); }

    private Models.Teacher saveTeacher(Models.Teacher t) {
        if (blank(t.name)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        boolean auto = blank(t.teacherId);
        if (auto) t.teacherId = null;
        Models.Teacher saved = teachers.save(t);
        if (auto) { saved.teacherId = String.format("TCH-%04d", saved.id); saved = teachers.save(saved); }
        return saved;
    }

    // ---------- Dashboard ----------
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Long> byDept = new LinkedHashMap<>();
        for (Object[] r : students.byDepartment()) byDept.put(String.valueOf(r[0]), (Long) r[1]);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("students", students.count());
        out.put("teachers", teachers.count());
        out.put("departments", byDept);
        out.put("recentStudents", students.findTop5ByOrderByIdDesc());
        out.put("recentTeachers", teachers.findTop5ByOrderByIdDesc());
        return out;
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    // ---------- Errors as {"error": "..."} ----------
    @RestControllerAdvice
    public static class Errors {
        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", String.valueOf(e.getReason())));
        }
        @ExceptionHandler(DataIntegrityViolationException.class)
        ResponseEntity<Map<String, String>> dup(DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "That Roll no / ID is already in use."));
        }
    }

    // ---------- Require a token for every /api call except login ----------
    @Component
    public static class AuthFilter implements Filter {
        @Override
        public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) rq;
            HttpServletResponse res = (HttpServletResponse) rs;
            String path = req.getRequestURI();
            if (path.startsWith("/api/") && !path.equals("/api/login")) {
                String h = req.getHeader("Authorization");
                if (h == null || !TOKENS.containsKey(h.replace("Bearer ", ""))) {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Please sign in again.\"}");
                    return;
                }
            }
            chain.doFilter(rq, rs);
        }
    }
}
