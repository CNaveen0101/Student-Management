const express = require('express');
const Database = require('better-sqlite3');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const path = require('path');

const SECRET = process.env.JWT_SECRET || 'change-this-secret';
const PORT = process.env.PORT || 3000;
const db = new Database(path.join(__dirname, 'school.db'));

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS students (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL, email TEXT, phone TEXT, grade TEXT,
  dob TEXT, guardian TEXT, address TEXT,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS teachers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL, email TEXT, phone TEXT, subject TEXT,
  qualification TEXT, hire_date TEXT,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);
`);

// Default admin account (change the password after first login)
if (!db.prepare('SELECT 1 FROM users LIMIT 1').get()) {
  db.prepare('INSERT INTO users (username, password_hash) VALUES (?, ?)')
    .run('admin', bcrypt.hashSync('admin123', 10));
  console.log('Created default user: admin / admin123');
}

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// ---- Auth ----
app.post('/api/login', (req, res) => {
  const { username, password } = req.body || {};
  const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
  if (!user || !bcrypt.compareSync(password || '', user.password_hash)) {
    return res.status(401).json({ error: 'Wrong username or password.' });
  }
  const token = jwt.sign({ id: user.id, username }, SECRET, { expiresIn: '8h' });
  res.json({ token, username });
});

function auth(req, res, next) {
  const token = (req.headers.authorization || '').replace('Bearer ', '');
  try { req.user = jwt.verify(token, SECRET); next(); }
  catch { res.status(401).json({ error: 'Please sign in again.' }); }
}

// ---- Generic CRUD for students and teachers ----
const tables = {
  students: ['name', 'email', 'phone', 'grade', 'dob', 'guardian', 'address'],
  teachers: ['name', 'email', 'phone', 'subject', 'qualification', 'hire_date'],
};

for (const [table, cols] of Object.entries(tables)) {
  const pick = (body) => cols.map((c) => (body[c] ?? '').toString().trim());
  const valid = (body) => (body.name || '').trim().length > 0;

  app.get(`/api/${table}`, auth, (req, res) => {
    const q = `%${req.query.q || ''}%`;
    const where = cols.map((c) => `${c} LIKE @q`).join(' OR ');
    res.json(db.prepare(`SELECT * FROM ${table} WHERE ${where} ORDER BY name`).all({ q }));
  });

  app.post(`/api/${table}`, auth, (req, res) => {
    if (!valid(req.body)) return res.status(400).json({ error: 'Name is required.' });
    const info = db.prepare(
      `INSERT INTO ${table} (${cols.join(',')}) VALUES (${cols.map(() => '?').join(',')})`
    ).run(...pick(req.body));
    res.status(201).json(db.prepare(`SELECT * FROM ${table} WHERE id = ?`).get(info.lastInsertRowid));
  });

  app.put(`/api/${table}/:id`, auth, (req, res) => {
    if (!valid(req.body)) return res.status(400).json({ error: 'Name is required.' });
    const info = db.prepare(
      `UPDATE ${table} SET ${cols.map((c) => `${c} = ?`).join(',')} WHERE id = ?`
    ).run(...pick(req.body), req.params.id);
    if (!info.changes) return res.status(404).json({ error: 'Not found.' });
    res.json(db.prepare(`SELECT * FROM ${table} WHERE id = ?`).get(req.params.id));
  });

  app.delete(`/api/${table}/:id`, auth, (req, res) => {
    db.prepare(`DELETE FROM ${table} WHERE id = ?`).run(req.params.id);
    res.status(204).end();
  });
}

app.get('/api/stats', auth, (req, res) => {
  res.json({
    students: db.prepare('SELECT COUNT(*) n FROM students').get().n,
    teachers: db.prepare('SELECT COUNT(*) n FROM teachers').get().n,
  });
});

app.listen(PORT, () => console.log(`Running at http://localhost:${PORT}`));
