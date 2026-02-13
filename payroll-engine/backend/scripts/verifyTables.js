#!/usr/bin/env node
/**
 * Verify all required database tables exist and have data
 */

const { getDatabase, initDatabase } = require('../database/init');
const { seedFederalData2026 } = require('../database/seedData');
const { importCompletePub15TTables } = require('./importPub15TComplete');

async function verifyTables() {
    try {
        // Initialize database first
        await initDatabase();
        await seedFederalData2026();
        
        const db = getDatabase();
        
        const tables = [
            'tax_years',
            'federal_brackets',
            'federal_deductions',
            'fica_rates',
            'pub15t_percentage_tables',
            'state_brackets',
            'state_deductions',
            'state_withholding_tables',
            'state_payroll_taxes',
            'local_taxes',
            'payroll_calculations_audit'
        ];
        
        console.log('\n=== Database Table Verification ===\n');
        
        for (const table of tables) {
            await new Promise((resolve, reject) => {
                db.get(`SELECT COUNT(*) as count FROM ${table}`, (err, row) => {
                    if (err) {
                        console.log(`❌ ${table}: ERROR - ${err.message}`);
                    } else {
                        const count = row.count;
                        const status = count > 0 ? '✅' : '⚠️';
                        console.log(`${status} ${table}: ${count} rows`);
                    }
                    resolve();
                });
            });
        }
        
        // Check specific data
        console.log('\n=== Data Verification ===\n');
        
        // Check FICA rates for 2026
        await new Promise((resolve) => {
            db.get('SELECT * FROM fica_rates WHERE year = 2026', (err, row) => {
                if (err || !row) {
                    console.log('❌ FICA rates for 2026: MISSING');
                } else {
                    console.log('✅ FICA rates for 2026:');
                    console.log(`   Social Security Rate: ${row.social_security_rate}`);
                    console.log(`   Social Security Wage Base: $${row.social_security_wage_base}`);
                    console.log(`   Medicare Rate: ${row.medicare_rate}`);
                    console.log(`   Additional Medicare Rate: ${row.additional_medicare_rate}`);
                    console.log(`   Additional Medicare Threshold: $${row.additional_medicare_threshold}`);
                }
                resolve();
            });
        });
        
        // Check Pub 15-T tables
        await new Promise((resolve) => {
            db.get('SELECT COUNT(*) as count FROM pub15t_percentage_tables WHERE year = 2026', (err, row) => {
                if (err) {
                    console.log('❌ Pub 15-T tables for 2026: ERROR');
                } else {
                    console.log(`✅ Pub 15-T tables for 2026: ${row.count} entries`);
                }
                resolve();
            });
        });
        
        // Check state withholding tables
        await new Promise((resolve) => {
            db.get('SELECT COUNT(DISTINCT state_code) as states FROM state_withholding_tables WHERE year = 2026', (err, row) => {
                if (err) {
                    console.log('❌ State withholding tables: ERROR');
                } else {
                    console.log(`⚠️  State withholding tables for 2026: ${row.states || 0} states (may need import)`);
                }
                resolve();
            });
        });
        
        console.log('\n=== Verification Complete ===\n');
        process.exit(0);
    } catch (error) {
        console.error('Verification error:', error);
        process.exit(1);
    }
}

verifyTables();

