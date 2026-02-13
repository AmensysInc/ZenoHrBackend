const { initDatabase } = require('../database/init');
const { seedFederalData2026, seedPub15TTables2026, seedColoradoData2026 } = require('../database/seedData');

async function initialize() {
    try {
        console.log('Initializing database...');
        await initDatabase();
        
        console.log('Seeding federal data...');
        await seedFederalData2026();
        
        console.log('Seeding Pub 15-T tables...');
        await seedPub15TTables2026();
        
        console.log('Seeding Colorado data...');
        await seedColoradoData2026();
        
        console.log('Database initialization complete!');
        process.exit(0);
    } catch (error) {
        console.error('Initialization error:', error);
        process.exit(1);
    }
}

initialize();

