const path = require('path');
const fs = require('fs');

const dbPath = path.join(__dirname, '..', 'database', 'tax_data.db');
const dbDir = path.dirname(dbPath);

console.log('📊 State Withholding Tax Records Location\n');
console.log(`Database Path: ${dbPath}`);
console.log(`Database Directory: ${dbDir}`);
console.log(`Absolute Path: ${path.resolve(dbPath)}\n`);

if (fs.existsSync(dbPath)) {
    const stats = fs.statSync(dbPath);
    const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);
    console.log(`✅ Database exists`);
    console.log(`   Size: ${sizeMB} MB`);
    console.log(`   Created: ${stats.birthtime}`);
    console.log(`   Modified: ${stats.mtime}\n`);
    
    // Check table
    const { getDatabase } = require('../database/init');
    const db = getDatabase();
    
    db.get(`SELECT COUNT(*) as count FROM state_withholding_tables WHERE year = 2026`, (err, row) => {
        if (err) {
            console.error('Error querying database:', err);
        } else {
            console.log(`📋 Records in state_withholding_tables (2026): ${row.count.toLocaleString()}`);
        }
        
        db.get(`SELECT COUNT(DISTINCT state_code) as states FROM state_withholding_tables WHERE year = 2026`, (err2, row2) => {
            if (!err2) {
                console.log(`📋 States with data: ${row2.states}`);
            }
            
            process.exit(0);
        });
    });
} else {
    console.log('❌ Database file not found');
    console.log(`   Expected location: ${dbPath}`);
    process.exit(1);
}

