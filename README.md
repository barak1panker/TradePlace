<div align="center">

# 📈 StockTrader

### Stock Market Trading Platform - Android App with Backend Server

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-ED8B00?logo=openjdk&logoColor=white)
![Server](https://img.shields.io/badge/server-Node.js-339933?logo=node.js&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-21-blue)
![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue)
![License](https://img.shields.io/badge/license-Educational-green)

*A complete trading simulation app with live market data, financial news, and portfolio management*

[Overview](#-overview) • [Installation](#-installation--setup) • [Architecture](#-architecture) • [API](#-api-endpoints) • [Troubleshooting](#-troubleshooting)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Installation & Setup](#-installation--setup)
  - [Step 1: Run the Server](#step-1-run-the-server-)
  - [Step 2: Run the App](#step-2-run-the-app-)
  - [Step 3: First Use](#step-3-first-use-)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [App Screens](#-app-screens)
- [API Endpoints](#-api-endpoints)
- [Data Storage](#-data-storage)
- [External APIs](#-external-apis)
- [Tech Stack](#-tech-stack)
- [Security](#-security)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Overview

**StockTrader** is a complete stock market trading simulation application, composed of two components:

<table>
<tr>
<td width="50%" align="center">

### 📱 Android Application
<br>
Written in Java
<br>
Full UI in Hebrew
<br>
Supports API 21+

</td>
<td width="50%" align="center">

### 🌐 Backend Server
<br>
Node.js + Express
<br>
REST API
<br>
JSON-based storage

</td>
</tr>
</table>

The app lets a trader open an account, log in with a password, buy and sell stocks, track portfolio performance, and receive live information from real market and news APIs. All user data is stored on a remote server, not just on the device.

---

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 🔐 User Management
- New account registration with validation
- Secure login with password hashing (SHA-256 + Salt)
- Remote server storage
- Multi-user support on the same device
- Automatic migration from local storage to server

</td>
<td width="50%" valign="top">

### 📊 Trading
- 15 leading US stocks
- Buy and sell with budget checks
- Quick quantity buttons (10/50/Max, 25%/50%/100%)
- Live cost and P&L calculation
- Confirmation dialog before every action

</td>
</tr>
<tr>
<td valign="top">

### 📈 Live Market Data
- 20-day chart from Alpha Vantage API
- Financial news from NewsAPI
- Live market simulation (updates every 3 seconds)
- Smart caching to avoid API overload

</td>
<td valign="top">

### 💼 Portfolio Management
- Automatic weighted average cost calculation
- Personal watchlist
- Complete transaction history
- P&L analysis (realized and unrealized)
- Portfolio breakdown pie chart

</td>
</tr>
</table>

---

## 🚀 Installation & Setup

### Prerequisites

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| Android Studio | Hedgehog 2023.1.1 | App development & runtime |
| JDK | 17 | Compilation |
| Android SDK | API 34 | Target SDK |
| Node.js | 16+ | Server runtime |

### Step 1: Run the Server ⚙️

Open **CMD** (not PowerShell - see [Troubleshooting](#-troubleshooting)) and navigate to the server folder:

```bash
cd StockTraderServer
```

**First time only** - install dependencies:

```bash
npm install
```

**Start the server**:

```bash
npm start
```

✅ If everything is OK, you'll see:

```
StockTrader server listening on port 3000
Health check: http://localhost:3000/
DB file:      C:\...\StockTraderServer\db.json
Accounts loaded: 0
```

**Health check**: open your browser at **http://localhost:3000/**

> ⚠️ **Important**: Keep the server window open while the app is running. Closing it = server stops.

### Step 2: Run the App 📱

1. Open **Android Studio**
2. `File → Open` and select the **`StockTraderApp`** folder
3. Wait for **Gradle Sync** (~2-10 minutes on first run - downloads dependencies)
4. Run on an **emulator** (click ▶️ or `Shift+F10`)

> 🌐 **For a real device**: edit `BASE_URL` in `ApiAuthClient.java` to your host machine's IP. See [Troubleshooting](#-troubleshooting).

### Step 3: First Use 🎉

1. On the login screen tap **"Create New Account"**
2. Fill in:
   - **Username**: 3-20 characters (letters/digits/underscore)
   - **Full name**: e.g. "John Doe"
   - **Password**: at least 6 characters
   - **Confirm password**: same password
3. You get **$100,000 virtual** to start trading!

---

## 🏗 Architecture

The app is built in an **MVC** architecture with the **Repository** pattern, organized into clear layers:

```
┌──────────────────────────────────────────────────────────────┐
│                    🎨 Activities (UI Layer)                  │
│   Splash → Login → Main → StockDetail → Buy/Sell → ...       │
└──────────────────────────────────────────────────────────────┘
                            ⬇
┌──────────────────────────────────────────────────────────────┐
│                  🔌 Adapters (RecyclerView)                  │
│       StockAdapter • HoldingAdapter • TransactionAdapter     │
│                       • NewsAdapter                          │
└──────────────────────────────────────────────────────────────┘
                            ⬇
┌──────────────────────────────────────────────────────────────┐
│                 💼 Data Layer (Singletons)                   │
│   DataRepository • AuthManager • ApiClient • PriceSimulator  │
│                       • ApiAuthClient                        │
└──────────────────────────────────────────────────────────────┘
                            ⬇
┌──────────────────────────────────────────────────────────────┐
│                   📦 Models (POJOs)                          │
│   Stock • Holding • Transaction • User • UserAccount         │
│              • Portfolio • NewsArticle                       │
└──────────────────────────────────────────────────────────────┘
                            ⬇ (HTTP)
┌──────────────────────────────────────────────────────────────┐
│   🌐 Backend Server (Node.js + Express + JSON DB)            │
└──────────────────────────────────────────────────────────────┘
```

### Guiding Principles

- 🎯 **Separation of concerns**: each layer owns one responsibility only
- 🧪 **Dumb Activities**: only render and forward clicks
- 🔁 **Singletons for services**: global access without Dependency Injection
- 🛡 **Errors as return values**: business logic returns String instead of throwing exceptions
- 🌐 **Server-first**: all user data is stored on the server, not just on the device

---

## 📁 Project Structure

```
StockTrader/
│
├── 📱 StockTraderApp/                    ← Android application
│   ├── app/src/main/
│   │   ├── java/com/example/stocktrader/
│   │   │   ├── 🎨 activities/            ← 10 screens
│   │   │   │   ├── SplashActivity
│   │   │   │   ├── LoginActivity
│   │   │   │   ├── RegisterActivity
│   │   │   │   ├── MainActivity
│   │   │   │   ├── StockDetailActivity
│   │   │   │   ├── BuyStockActivity
│   │   │   │   ├── SellStockActivity
│   │   │   │   ├── PortfolioActivity
│   │   │   │   ├── WatchlistActivity
│   │   │   │   ├── TransactionsActivity
│   │   │   │   └── AnalyticsActivity
│   │   │   ├── 🔌 adapters/              ← RecyclerView Adapters
│   │   │   ├── 💼 data/                  ← Data layer
│   │   │   │   ├── DataRepository       ← Central state holder
│   │   │   │   ├── AuthManager          ← Account management
│   │   │   │   ├── ApiClient            ← Alpha Vantage + NewsAPI
│   │   │   │   ├── ApiAuthClient        ← Local backend
│   │   │   │   ├── PriceSimulator       ← Live market sim
│   │   │   │   └── PortfolioSerializer  ← JSON ↔ Portfolio
│   │   │   ├── 📦 models/                ← POJO Models
│   │   │   └── 🛠 utils/                 ← Helpers
│   │   └── res/                          ← Layouts, Drawables, Strings
│   └── build.gradle
│
└── 🌐 StockTraderServer/                 ← Backend server
    ├── 📄 server.js                      ← Main server file
    ├── 📄 package.json                   ← Dependencies
    └── 📄 db.json                        ← Database (auto-generated)
```

---

## 🖥 App Screens

| Screen | Description |
|--------|-------------|
| 🚪 **Splash** | Opening screen with logo (1.2 seconds) |
| 🔑 **Login** | Sign in with username and password |
| ✍️ **Register** | Create a new account |
| 🏠 **Main** | Home screen - portfolio value, stock search, navigation |
| 📊 **Stock Detail** | Stock details + chart + news + actions |
| 💰 **Buy** | Buy a stock with live calculations |
| 💵 **Sell** | Sell a stock with P&L calculation |
| 💼 **Portfolio** | List of open positions |
| ⭐ **Watchlist** | Favorite stocks |
| 📜 **Transactions** | Transaction history |
| 📈 **Analytics** | Performance analytics + pie chart |

---

## 🔌 API Endpoints

### 🟢 Server Health

<table>
<tr><th>Method</th><th>Path</th><th>Description</th></tr>
<tr><td><code>GET</code></td><td><code>/</code></td><td>Health check + account count</td></tr>
<tr><td><code>GET</code></td><td><code>/api/users</code></td><td>List of usernames (debug)</td></tr>
</table>

### 🔐 Authentication

<table>
<tr><th>Method</th><th>Path</th><th>Description</th></tr>
<tr><td><code>POST</code></td><td><code>/api/register</code></td><td>Create a new account</td></tr>
<tr><td><code>POST</code></td><td><code>/api/login</code></td><td>Sign in</td></tr>
</table>

### 💼 Accounts

<table>
<tr><th>Method</th><th>Path</th><th>Description</th></tr>
<tr><td><code>GET</code></td><td><code>/api/account/:username</code></td><td>Get full account info</td></tr>
<tr><td><code>PUT</code></td><td><code>/api/account/:username</code></td><td>Update balance and portfolio</td></tr>
</table>

### Usage Examples

<details>
<summary><b>📝 Register a new account</b></summary>

**Request:**
```bash
curl -X POST http://localhost:3000/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "fullName": "John Doe",
    "password": "secret123"
  }'
```

**Response (success):**
```json
{
  "ok": true,
  "account": {
    "username": "john_doe",
    "fullName": "John Doe",
    "cashBalance": 100000,
    "portfolioJson": null
  }
}
```

**Response (failure):**
```json
{
  "error": "Username already exists"
}
```
</details>

<details>
<summary><b>🔑 Login</b></summary>

**Request:**
```bash
curl -X POST http://localhost:3000/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secret123"
  }'
```

**Response:**
```json
{
  "ok": true,
  "account": {
    "username": "john_doe",
    "fullName": "John Doe",
    "cashBalance": 100000,
    "portfolioJson": null
  }
}
```
</details>

<details>
<summary><b>💾 Update balance and portfolio</b></summary>

**Request:**
```bash
curl -X PUT http://localhost:3000/api/account/john_doe \
  -H "Content-Type: application/json" \
  -d '{
    "cashBalance": 85000.50,
    "portfolioJson": "{\"holdings\":[{\"symbol\":\"AAPL\",\"quantity\":10}]}"
  }'
```
</details>

---

## 💾 Data Storage

### Location

Data is stored in the **`db.json`** file inside the server folder. It's created automatically on first run.

### File Structure

```json
{
  "accounts": {
    "john_doe": {
      "username": "john_doe",
      "fullName": "John Doe",
      "passwordHash": "a3f5c1e8...",
      "cashBalance": 87654.32,
      "portfolioJson": "{\"holdings\":[...]}",
      "createdAt": 1717092000000,
      "updatedAt": 1717105500000
    }
  }
}
```

### Viewing the Data

Open `db.json` with any text editor (Notepad, VS Code) to see all the data.

### Reset

To delete all data:
```bash
# 1. Stop the server (Ctrl+C)
# 2. Delete the file
del db.json     # Windows
rm db.json      # Mac/Linux
# 3. Restart - file will be recreated empty
npm start
```

---

## 🌐 External APIs

### 📈 Alpha Vantage - Market Data

```
GET https://www.alphavantage.co/query
    ?function=TIME_SERIES_DAILY
    &symbol=AAPL
    &apikey=YOUR_KEY
```

- **Used for**: 20-day price chart for each stock
- **Limit**: 25 calls per day on the free tier
- **Cache**: 15 minutes in memory

### 📰 NewsAPI - Financial News

```
GET https://newsapi.org/v2/everything
    ?q=Apple Inc.
    &sortBy=relevancy
    &language=en
    &pageSize=5
    &apiKey=YOUR_KEY
```

- **Used for**: 5 latest news articles per stock
- **Limit**: 100 calls per day on the free tier
- **Cache**: 30 minutes in memory

---

## 🛠 Tech Stack

<table>
<tr>
<th width="50%">📱 App</th>
<th width="50%">🌐 Server</th>
</tr>
<tr>
<td valign="top">

- **Language**: Java 8
- **Platform**: Android (API 21-34)
- **UI**: AndroidX (AppCompat, Material)
- **Lists**: RecyclerView + CardView
- **Charts**: MPAndroidChart 3.1.0
- **Network**: HttpURLConnection + ExecutorService
- **JSON**: org.json (built-in)
- **Hashing**: SHA-256 (java.security)

</td>
<td valign="top">

- **Runtime**: Node.js 16+
- **Framework**: Express.js 4.18
- **Body Parsing**: body-parser
- **CORS**: cors
- **Storage**: JSON file (no DB needed)
- **Hashing**: crypto (built-in) - SHA-256
- **Logging**: Console
- **Dependencies**: just 3 packages

</td>
</tr>
</table>

---

## 🔐 Security

### What's Protected ✅

- Passwords are stored as **SHA-256 hash** with **salt** (username)
- The password never travels back to the client
- Error messages don't reveal whether the username exists or the password is wrong
- Server-side validation (not only on the client)
- Password validity check before sending over the network
- On Android: all files are sandboxed by the Application Sandbox

### What's Not Protected (Educational Setting) ❌

- No HTTPS - traffic is plain HTTP
- No rate limiting on login (vulnerable to brute force)
- No JWT/Sessions - each request is identified only by username
- No CSRF protection
- Weak salt (the username) - not bcrypt/scrypt

> ⚠️ **This system is for development/learning only**. For production, all of the above must be added.

---

## 🆘 Troubleshooting

<details>
<summary><b>🔴 "Network error" when logging in</b></summary>

**Reason**: the server isn't running.

**Fix**: 
1. Open a CMD window
2. Navigate to `StockTraderServer`
3. Run `npm start`
4. Make sure you see: `StockTrader server listening on port 3000`
</details>

<details>
<summary><b>🔴 PowerShell blocks npm</b></summary>

**Error**: `npm.ps1 cannot be loaded because running scripts is disabled on this system`

**Quick fix** (recommended): use regular **CMD** instead of PowerShell.

**Permanent fix**: run in PowerShell:
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```
Answer `Y` to confirm.
</details>

<details>
<summary><b>🔴 Cannot find module 'express'</b></summary>

**Reason**: you haven't run `npm install`.

**Fix**:
```bash
cd StockTraderServer
npm install
npm start
```
</details>

<details>
<summary><b>🔴 Port 3000 is busy</b></summary>

**Option 1**: close the app holding the port (probably a previous server instance).

**Option 2**: change the port in `server.js`:
```javascript
const PORT = 3001;  // or any other port
```
And update `BASE_URL` in `ApiAuthClient.java` accordingly:
```java
public static final String BASE_URL = "http://10.0.2.2:3001";
```
</details>

<details>
<summary><b>🔴 "non-ASCII characters" in Gradle Sync</b></summary>

**Reason**: the project path contains non-English characters.

**Fix**: move the project to an English-only path, for example:
```
C:\AndroidProjects\StockTraderApp
```
</details>

<details>
<summary><b>🔴 Stock chart doesn't load</b></summary>

**Reason**: usually you've hit the Alpha Vantage limit (25 calls/day).

**Fix**: 
- Wait until tomorrow
- Or replace the API key in `ApiClient.java`
- The app will show a fallback chart from the local simulator
</details>

<details>
<summary><b>🔴 Real device can't connect to the server</b></summary>

**Fix**: 
1. Make sure the phone and the computer are on the same WiFi
2. Find the computer's IP (`ipconfig` in CMD)
3. Edit `BASE_URL` in `ApiAuthClient.java`:
   ```java
   public static final String BASE_URL = "http://192.168.1.10:3000";
   ```
4. Open port 3000 in Windows Firewall:
   - Control Panel → Windows Defender Firewall
   - Advanced Settings → Inbound Rules → New Rule
   - Port → TCP 3000 → Allow
</details>

<details>
<summary><b>🔴 App crashes on startup</b></summary>

**Checklist**:
1. ✅ Did Gradle Sync succeed?
2. ✅ Are you running on an emulator (not a real device without an IP setting)?
3. ✅ Open **Logcat** in Android Studio and filter by `AndroidRuntime` - it will show you the exact stack trace
</details>

---

## 📚 Credits

- [Alpha Vantage](https://www.alphavantage.co/) - live market data
- [NewsAPI](https://newsapi.org/) - financial news
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - charts library
- [Material Icons](https://fonts.google.com/icons) - vector icons
- [Express.js](https://expressjs.com/) - server framework

---

## 📜 License

Educational project - free to use and extend.

---

<div align="center">

### Built ❤️ for an educational project at Holon Institute of Technology

**🚀 Ready to go!**

[Back to top ⬆](#-stocktrader)

</div>
