# Port Configuration

## ✅ Port Configuration Updated

### Frontend
- **New Port:** 3001
- **URL:** http://localhost:3001
- **Previous Port:** 3000 (was in use)
- **Configuration:** Added `PORT=3001` to `.env` file

### Backend
- **Port:** 8082 (unchanged)
- **URL:** http://localhost:8082

## 🌐 Access URLs

- **Frontend:** http://localhost:3001
- **Backend API:** http://localhost:8082

## 📝 Environment Variables

The frontend `.env` file now contains:
```
REACT_APP_API_URL=http://localhost:8082
PORT=3001
```

## 🔄 Restarting Services

If you need to restart:

### Frontend (Port 3001):
```powershell
cd frontend
npm start
```
Or set PORT explicitly:
```powershell
cd frontend
$env:PORT=3001; npm start
```

### Backend (Port 8082):
```powershell
.\mvnw.cmd spring-boot:run
```

## ⚠️ Note

The frontend has been restarted on port 3001. The browser should automatically open to http://localhost:3001 once it compiles.

If the browser doesn't auto-open, manually navigate to: **http://localhost:3001**

