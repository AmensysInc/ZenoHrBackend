const { getDatabase } = require('../database/init');

/**
 * Migrate existing Pub 15-T data to new schema
 * Converts base_amount/percentage to base_tax/rate/excess_over
 */
async function migratePub15TSchema() {
    const db = getDatabase();
    
    return new Promise((resolve, reject) => {
        db.serialize(() => {
            // Check if migration is needed
            db.get(`SELECT COUNT(*) as count FROM pub15t_percentage_tables WHERE base_tax IS NULL AND base_amount IS NOT NULL`, [], (err, row) => {
                if (err) {
                    reject(err);
                    return;
                }
                
                if (row.count === 0) {
                    console.log('No migration needed - data already in new format');
                    resolve();
                    return;
                }
                
                console.log(`Migrating ${row.count} rows to new schema...`);
                
                // Update existing rows: convert base_amount/percentage to base_tax/rate/excess_over
                // For existing data, excess_over = wage_min, base_tax = base_amount, rate = percentage
                db.run(`UPDATE pub15t_percentage_tables 
                        SET base_tax = base_amount,
                            rate = percentage,
                            excess_over = wage_min
                        WHERE base_tax IS NULL AND base_amount IS NOT NULL`, (err2) => {
                    if (err2) {
                        reject(err2);
                        return;
                    }
                    
                    console.log('✅ Migration complete');
                    resolve();
                });
            });
        });
    });
}

if (require.main === module) {
    migratePub15TSchema()
        .then(() => {
            const db = getDatabase();
            db.close();
            process.exit(0);
        })
        .catch(err => {
            console.error('Migration error:', err);
            process.exit(1);
        });
}

module.exports = { migratePub15TSchema };

