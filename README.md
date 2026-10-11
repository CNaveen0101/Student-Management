# Student Information App

Node.js + Express + SQLite (file `school.db`, created automatically).

## Run
    npm install
    npm start
Open http://localhost:3000 and sign in with **admin / admin123** (change this after first login).
Set a real secret before deploying: `JWT_SECRET=your-long-random-string npm start`

## API (all except login need `Authorization: Bearer <token>`)
- POST /api/login
- GET/POST /api/students, PUT/DELETE /api/students/:id  (GET supports ?q=search)
- GET/POST /api/teachers, PUT/DELETE /api/teachers/:id
- GET /api/stats
