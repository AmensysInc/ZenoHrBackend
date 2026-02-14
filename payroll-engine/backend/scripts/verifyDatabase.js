#!/usr/bin/env node
/**
 * Verify database seeding - check table row counts
 * Usage: node scripts/verifyDatabase.js
 */

const { getDatabase } = require('../database/init');

async function verifyDatabase() {
    const db = getDatabase();
    const year = 2026;
    
    console.log('==========================================');
    console.log('Database Verification Report');
    console.log('==========================================\n');
    
    const tables = [
        { name: 'tax_years', description: 'Tax Years' },
        { name: 'fica_rates', description: 'FICA Rates', yearFilter: true },
        { name: 'federal_deductions', description: 'Federal Deductions', yearFilter: true },
        { name: 'federal_brackets', description: 'Federal Brackets', yearFilter: true },
        { name: 'pub15t_percentage_tables', description: 'Pub 15-T Percentage Tables', yearFilter: true },
        { name: 'state_brackets', description: 'State Brackets', yearFilter: true },
        { name: 'state_deductions', description: 'State Deductions', yearFilter: true },
        { name: 'state_withholding_tables', description: 'State Withholding Tables', yearFilter: true },
        { name: 'state_payroll_taxes', description: 'State Payroll Taxes', yearFilter: true },
        { name: 'local_taxes', description: 'Local Taxes', yearFilter: true }
    ];
    
    let totalRows = 0;
    let allGood = true;
    
    for (const table of tables) {
        const yearFilter = table.yearFilter ? ` WHERE year = ${year}` : '';
        const query = `SELECT COUNT(*) as count FROM ${table.name}${yearFilter}`;
        
        await new Promise((resolve, reject) => {
            db.get(query, (err, row) => {
                if (err) {
                    console.error(`❌ Error checking ${table.name}: ${err.message}`);
                    allGood = false;
                    resolve();
                    return;
                }
                
                const count = row ? row.count : 0;
                totalRows += count;
                
                const status = count > 0 ? '✓' : '✗';
                const statusColor = count > 0 ? '\x1b[32m' : '\x1b[31m';
                const resetColor = '\x1b[0m';
                
                console.log(`${statusColor}${status}${resetColor} ${table.description.padEnd(35)} ${count.toString().padStart(10)} rows`);
                
                if (count === 0 && table.name !== 'local_taxes' && table.name !== 'state_payroll_taxes') {
                    allGood = false;
                }
                
                resolve();
            });
        });
    }
    
    console.log('\n==========================================');
    console.log(`Total Rows: ${totalRows.toLocaleString()}`);
    console.log('==========================================\n');
    
    if (allGood) {
        console.log('✅ Database is properly seeded!');
        console.log('All critical tables have data.\n');
        process.exit(0);
    } else {
        console.log('⚠️  Some tables are empty or missing data.');
        console.log('Run: node scripts/seedDatabase.js to seed the database.\n');
        process.exit(1);
    }
}

verifyDatabase().catch(err => {
    console.error('Verification error:', err);
    process.exit(1);
});

