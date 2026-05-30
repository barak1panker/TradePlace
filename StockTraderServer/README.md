# StockTrader Server

שרת Node.js שמספק backend לאפליקציית StockTrader.

## דרישות
- Node.js 16 ומעלה ([הורדה](https://nodejs.org/))

## הפעלה

```bash
cd StockTraderServer
npm install
npm start
```

השרת ירוץ על פורט 3000. בדיקת תקינות: פתח דפדפן וגש ל-`http://localhost:3000/`.

## חיבור האפליקציה

### אם רצים על אמולטור Android Studio
האפליקציה מגיעה מוגדרת מראש לכתובת `http://10.0.2.2:3000` שזו הכתובת המיוחדת שאמולטור משתמש בה כדי לגשת ל-localhost של המחשב המארח.

### אם רצים על מכשיר אמיתי
1. הטלפון והמחשב חייבים להיות באותה רשת WiFi
2. גלה את ה-IP של המחשב (`ipconfig` ב-Windows, `ifconfig` ב-Mac/Linux)
3. ערוך את הקבוע `BASE_URL` ב-`ApiAuthClient.java` באפליקציה ל-`http://<IP-של-המחשב>:3000`
4. אם יש Windows Firewall, אפשר חיבורים נכנסים על פורט 3000

## נקודות קצה

| Method | Path | תיאור |
|--------|------|--------|
| GET | `/` | בדיקת תקינות |
| GET | `/api/users` | רשימת שמות משתמשים (debug) |
| POST | `/api/register` | יצירת חשבון חדש |
| POST | `/api/login` | התחברות |
| GET | `/api/account/:username` | קבלת חשבון מלא |
| PUT | `/api/account/:username` | עדכון יתרה ותיק |

## אחסון

הכל נשמר בקובץ `db.json` באותה תיקייה. אפשר לפתוח אותו עם כל text editor כדי לראות את המידע. אפס את הכל - פשוט מחק את הקובץ והשרת ייצור אותו מחדש.

## עצירה

`Ctrl+C` בטרמינל של השרת.
