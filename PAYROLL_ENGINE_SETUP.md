# Payroll Engine Integration - Repository Structure

## How Payroll Engine is Included

The **payroll engine** is included **directly in the backend repository** as a folder (`payroll-engine/`). It is NOT a git submodule - it's part of the main repository.

### Repository Structure

```
quick-hrms-backend-master/
├── payroll-engine/              # Payroll engine (included directly)
│   ├── backend/                 # Node.js backend service
│   │   ├── server.js           # Main server file
│   │   ├── package.json        # Dependencies
│   │   ├── Dockerfile          # Docker configuration
│   │   └── ...
│   ├── paystub_calculator.html  # Original calculator HTML
│   └── ...
├── frontend/                    # Frontend (git submodule)
│   ├── public/
│   │   └── payroll-engine-template.html  # Embedded version
│   └── src/
│       └── GeneratePayroll/   # React component
├── src/                         # Spring Boot backend
└── ...
```

### What's Included

1. **Payroll Engine Backend** (`payroll-engine/backend/`)
   - Node.js Express server
   - Tax calculation services
   - Database initialization scripts
   - All source code and dependencies

2. **Payroll Engine Frontend Template** (`frontend/public/payroll-engine-template.html`)
   - Modified version of the calculator HTML
   - Embedded in React app via iframe
   - Communicates with parent window via postMessage

3. **Integration Code**
   - `frontend/src/GeneratePayroll/GeneratePayroll.js` - React component
   - Backend endpoints in `PayrollController.java`
   - Database entities updated to store HTML templates

### What's NOT Included (in .gitignore)

- `payroll-engine/backend/node_modules/` - Dependencies (install with `npm install`)
- `payroll-engine/backend/database/*.db` - SQLite database files
- `payroll-engine/backend/data/` - Data files (if any)

### Running the Payroll Engine

#### Option 1: Standalone (Development)
```bash
cd payroll-engine/backend
npm install
npm run init-db
npm start
# Runs on http://localhost:3000
```

#### Option 2: Docker (Production)
```bash
# Using docker-compose.prod.yml
docker-compose -f docker-compose.prod.yml up payroll-engine

# Or build and run separately
cd payroll-engine/backend
docker build -t zenohr-payroll-engine .
docker run -p 3000:3000 zenohr-payroll-engine
```

### Production Endpoints

For production at `zenopayhr.com`:
- **Payroll Engine API**: `https://zenopayhr.com:3000`
- **Main Backend API**: `https://zenopayhr.com/api`
- **PDF Service**: `https://zenopayhr.com:3002` (optional, using client-side now)

### Notes

- The payroll engine is a **separate Node.js service** that runs independently
- It communicates with the main backend via HTTP API calls
- The frontend embeds the payroll calculator HTML in an iframe
- Generated paystubs are saved to the main database with HTML templates
- PDF generation uses **client-side jsPDF + html2canvas** (no server needed)

### Updating the Payroll Engine

If you need to update the payroll engine code:
1. Make changes in `payroll-engine/` folder
2. Commit and push to the main repository
3. The payroll engine is part of the main repo, so updates are included automatically

### Alternative: Git Submodule (Not Currently Used)

If you prefer to keep the payroll engine as a separate repository:
```bash
# Remove current folder
git rm -r payroll-engine

# Add as submodule
git submodule add https://github.com/AmensysInc/Zeno-Payroll-engine.git payroll-engine

# When cloning the repo
git submodule update --init --recursive
```

Currently, we're using the **direct inclusion** approach for simplicity.

