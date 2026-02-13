#!/usr/bin/env node
/**
 * Database seeding script for production
 * Run this ONCE during deployment to seed all required tax tables
 * 
 * Usage: node scripts/seedDatabase.js
 */

const { initDatabase } = require('../database/init');
const { seedFederalData2026 } = require('../database/seedData');
const { importCompletePub15TTables } = require('./importPub15TComplete');
const { validateTables } = require('../database/validateTables');

async function seedDatabase() {
    try {
        console.log('=== Starting Database Seeding ===\n');
        
        // Step 1: Initialize database (create tables)
        console.log('Step 1: Initializing database tables...');
        await initDatabase();
        console.log('✓ Database tables created\n');
        
        // Step 2: Seed federal data (FICA rates, brackets, deductions)
        console.log('Step 2: Seeding federal tax data...');
        await seedFederalData2026();
        console.log('✓ Federal tax data seeded\n');
        
        // Step 3: Import Pub 15-T tables
        console.log('Step 3: Importing Pub 15-T percentage tables...');
        await importCompletePub15TTables();
        console.log('✓ Pub 15-T tables imported\n');
        
        // Step 4: Validate all tables
        console.log('Step 4: Validating database...');
        const validation = await validateTables(2026);
        
        if (!validation.valid) {
            console.error('\n❌ Validation FAILED:');
            validation.errors.forEach(err => console.error('  -', err));
            process.exit(1);
        }
        
        if (validation.warnings.length > 0) {
            console.warn('\n⚠️  Validation Warnings:');
            validation.warnings.forEach(warn => console.warn('  -', warn));
        }
        
        console.log('\n✓ Database validation passed');
        console.log('\n=== Database Seeding Complete ===');
        process.exit(0);
        
    } catch (error) {
        console.error('\n❌ Database seeding failed:', error.message);
        console.error(error.stack);
        process.exit(1);
    }
}

seedDatabase();

