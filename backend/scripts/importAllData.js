const { initDatabase } = require('../database/init');
const { seedFederalData2026 } = require('../database/seedData');
const { importCompletePub15TTables } = require('./importPub15TComplete');
const { importStateData } = require('./importStateData');
const { importComprehensiveCountyTaxes } = require('./importComprehensiveCountyTaxes');

async function importAllData() {
    try {
        console.log('Initializing database...');
        await initDatabase();
        
        console.log('Seeding federal data...');
        await seedFederalData2026();
        
        console.log('Importing complete Pub 15-T tables...');
        await importCompletePub15TTables();
        
        console.log('Importing state data...');
        await importStateData();
        
        console.log('Importing comprehensive county/local taxes...');
        await importComprehensiveCountyTaxes();
        
        console.log('\n✅ All data imported successfully!');
        console.log('\n⚠️  Important Notes:');
        console.log('1. Pub 15-T tables are calculated from 2026 tax brackets.');
        console.log('   For exact matching, verify against official IRS Publication 15-T (2026).');
        console.log('2. Import state withholding tables from each state revenue department');
        console.log('3. Verify all rates and brackets against official 2026 sources');
        console.log('\n🚀 Next step: Start the server: npm start');
        
        process.exit(0);
    } catch (error) {
        console.error('Import error:', error);
        process.exit(1);
    }
}

importAllData();

