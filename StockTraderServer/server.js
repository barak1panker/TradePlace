/**
 * StockTrader backend server.
 *
 * Endpoints:
 *   POST /api/register       - create new account
 *   POST /api/login          - validate credentials
 *   GET  /api/account/:user  - get account + portfolio
 *   PUT  /api/account/:user  - update cash + portfolio
 *   GET  /api/users          - list all usernames (debug)
 *   GET  /                   - health check
 *
 * Data is persisted to db.json next to this file.
 */
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'db.json');
const PORT = 3000;
const STARTING_CASH = 100000.0;

// ===== Tiny JSON database =====
function loadDb() {
    try {
        if (!fs.existsSync(DB_PATH)) return { accounts: {} };
        const raw = fs.readFileSync(DB_PATH, 'utf8');
        return JSON.parse(raw);
    } catch (e) {
        console.error('DB load failed, starting fresh:', e.message);
        return { accounts: {} };
    }
}

function saveDb(db) {
    fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), 'utf8');
}

let db = loadDb();

// ===== Helpers =====
function sha256(salt, password) {
    return crypto.createHash('sha256')
        .update((salt || '').toLowerCase() + ':' + password)
        .digest('hex');
}

function validUsername(u) {
    return typeof u === 'string' && /^[a-zA-Z0-9_]{3,20}$/.test(u);
}

function validPassword(p) {
    return typeof p === 'string' && p.length >= 6;
}

function publicAccount(acc) {
    // Don't ever leak the passwordHash to the client.
    return {
        username: acc.username,
        fullName: acc.fullName,
        cashBalance: acc.cashBalance,
        portfolioJson: acc.portfolioJson || null,
    };
}

// ===== App setup =====
const app = express();
app.use(cors());
app.use(bodyParser.json({ limit: '2mb' }));

app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
    next();
});

// ===== Endpoints =====

app.get('/', (req, res) => {
    res.json({
        status: 'ok',
        service: 'StockTrader Server',
        accounts: Object.keys(db.accounts).length
    });
});

app.get('/api/users', (req, res) => {
    res.json({ users: Object.keys(db.accounts) });
});

app.post('/api/register', (req, res) => {
    const { username, fullName, password, passwordHash } = req.body || {};

    if (!validUsername(username)) {
        return res.status(400).json({
            error: 'שם המשתמש חייב להכיל 3-20 תווים באנגלית/ספרות/קו תחתון'
        });
    }
    if (!fullName || !fullName.trim()) {
        return res.status(400).json({ error: 'נדרש שם מלא' });
    }
    const key = username.toLowerCase();
    if (db.accounts[key]) {
        return res.status(409).json({ error: 'שם המשתמש כבר קיים במערכת' });
    }

    // Accept either a plain password (server hashes it) or a pre-computed hash
    // (used by the auto-migration flow from existing local accounts).
    let storedHash;
    if (passwordHash) {
        // Pre-hashed - 64 hex chars
        if (typeof passwordHash !== 'string' || passwordHash.length !== 64) {
            return res.status(400).json({ error: 'hash סיסמה לא תקין' });
        }
        storedHash = passwordHash;
    } else {
        if (!validPassword(password)) {
            return res.status(400).json({ error: 'הסיסמה חייבת להכיל לפחות 6 תווים' });
        }
        storedHash = sha256(username, password);
    }

    const acc = {
        username,
        fullName: fullName.trim(),
        passwordHash: storedHash,
        cashBalance: STARTING_CASH,
        portfolioJson: null,
        createdAt: Date.now(),
    };
    db.accounts[key] = acc;
    saveDb(db);

    res.status(201).json({ ok: true, account: publicAccount(acc) });
});

app.post('/api/login', (req, res) => {
    const { username, password } = req.body || {};
    if (!username || !password) {
        return res.status(400).json({ error: 'נדרש שם משתמש וסיסמה' });
    }
    const key = username.toLowerCase();
    const acc = db.accounts[key];
    if (!acc) {
        return res.status(401).json({ error: 'שם משתמש או סיסמה שגויים' });
    }
    const candidate = sha256(username, password);
    if (candidate !== acc.passwordHash) {
        return res.status(401).json({ error: 'שם משתמש או סיסמה שגויים' });
    }
    res.json({ ok: true, account: publicAccount(acc) });
});

app.get('/api/account/:username', (req, res) => {
    const key = (req.params.username || '').toLowerCase();
    const acc = db.accounts[key];
    if (!acc) return res.status(404).json({ error: 'משתמש לא נמצא' });
    res.json({ ok: true, account: publicAccount(acc) });
});

app.put('/api/account/:username', (req, res) => {
    const key = (req.params.username || '').toLowerCase();
    const acc = db.accounts[key];
    if (!acc) return res.status(404).json({ error: 'משתמש לא נמצא' });

    const { cashBalance, portfolioJson } = req.body || {};
    if (typeof cashBalance === 'number') acc.cashBalance = cashBalance;
    if (typeof portfolioJson === 'string') acc.portfolioJson = portfolioJson;
    acc.updatedAt = Date.now();
    saveDb(db);
    res.json({ ok: true, account: publicAccount(acc) });
});

// ===== Start =====
app.listen(PORT, '0.0.0.0', () => {
    console.log(`StockTrader server listening on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/`);
    console.log(`DB file:      ${DB_PATH}`);
    console.log(`Accounts loaded: ${Object.keys(db.accounts).length}`);
});
