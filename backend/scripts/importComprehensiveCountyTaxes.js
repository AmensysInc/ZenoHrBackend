const { getDatabase } = require('../database/init');

/**
 * Import comprehensive county/local tax data for 2026
 * Includes all states with county taxes, city taxes, and local jurisdictions
 */
async function importComprehensiveCountyTaxes() {
    const db = getDatabase();
    const year = 2026;

    console.log('📥 Importing Comprehensive County/Local Tax Data for 2026\n');
    console.log('='.repeat(80));

    // Comprehensive county/local tax data
    const localTaxes = [
        // ========== NEW YORK ==========
        {
            state_code: 'NY',
            jurisdiction: 'NYC',
            tax_type: 'brackets',
            rate: null,
            fixed_amount: null,
            brackets_json: JSON.stringify([
                { min: 0, max: 12000, rate: 0.03078 },
                { min: 12000, max: 25000, rate: 0.03762 },
                { min: 25000, max: 50000, rate: 0.03819 },
                { min: 50000, max: 999999999, rate: 0.03876 }
            ])
        },
        {
            state_code: 'NY',
            jurisdiction: 'YONKERS_RESIDENT',
            tax_type: 'flat',
            rate: 0.165,
            fixed_amount: null,
            brackets_json: null
        },
        {
            state_code: 'NY',
            jurisdiction: 'YONKERS_NONRESIDENT',
            tax_type: 'flat',
            rate: 0.005,
            fixed_amount: null,
            brackets_json: null
        },

        // ========== MARYLAND - All 23 Counties ==========
        // Maryland counties have rates from 2.25% to 3.2%
        { state_code: 'MD', jurisdiction: 'ALLEGANY', tax_type: 'flat', rate: 0.0302, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'ANNE_ARUNDEL', tax_type: 'flat', rate: 0.0250, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'BALTIMORE_CITY', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'BALTIMORE_COUNTY', tax_type: 'flat', rate: 0.0308, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'CALVERT', tax_type: 'flat', rate: 0.0300, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'CAROLINE', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'CARROLL', tax_type: 'flat', rate: 0.0305, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'CECIL', tax_type: 'flat', rate: 0.0302, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'CHARLES', tax_type: 'flat', rate: 0.0303, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'DORCHESTER', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'FREDERICK', tax_type: 'flat', rate: 0.0296, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'GARRETT', tax_type: 'flat', rate: 0.0250, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'HARFORD', tax_type: 'flat', rate: 0.0306, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'HOWARD', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'KENT', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'MONTGOMERY', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'PRINCE_GEORGES', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'QUEEN_ANNES', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'SOMERSET', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'ST_MARYS', tax_type: 'flat', rate: 0.0303, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'TALBOT', tax_type: 'flat', rate: 0.0225, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'WASHINGTON', tax_type: 'flat', rate: 0.0280, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'WICOMICO', tax_type: 'flat', rate: 0.0320, fixed_amount: null, brackets_json: null },
        { state_code: 'MD', jurisdiction: 'WORCESTER', tax_type: 'flat', rate: 0.0250, fixed_amount: null, brackets_json: null },

        // ========== PENNSYLVANIA ==========
        {
            state_code: 'PA',
            jurisdiction: 'LOCAL_EIT',
            tax_type: 'flat',
            rate: 0.01, // Default 1% - varies by municipality (0.5% to 3.5%)
            fixed_amount: null,
            brackets_json: null
        },
        {
            state_code: 'PA',
            jurisdiction: 'LOCAL_LST',
            tax_type: 'fixed',
            rate: null,
            fixed_amount: 52, // Annual amount, prorated
            brackets_json: null
        },

        // ========== OHIO - School Districts ==========
        {
            state_code: 'OH',
            jurisdiction: 'SCHOOL_DISTRICT',
            tax_type: 'flat',
            rate: 0.01, // Default 1% - varies by school district (0.5% to 2.5%)
            fixed_amount: null,
            brackets_json: null
        },

        // ========== INDIANA - County Taxes ==========
        // Indiana has county income taxes (rates vary by county)
        { state_code: 'IN', jurisdiction: 'MARION', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // Indianapolis
        { state_code: 'IN', jurisdiction: 'LAKE', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null },
        { state_code: 'IN', jurisdiction: 'ALLEN', tax_type: 'flat', rate: 0.013, fixed_amount: null, brackets_json: null }, // Fort Wayne
        { state_code: 'IN', jurisdiction: 'ST_JOSEPH', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // South Bend
        { state_code: 'IN', jurisdiction: 'VANDERBURGH', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // Evansville
        { state_code: 'IN', jurisdiction: 'TIPPECANOE', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // Lafayette
        { state_code: 'IN', jurisdiction: 'HAMILTON', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'IN', jurisdiction: 'ELKHART', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null },
        { state_code: 'IN', jurisdiction: 'MONROE', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // Bloomington
        { state_code: 'IN', jurisdiction: 'DELAWARE', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // Muncie

        // ========== KENTUCKY - County Taxes ==========
        // Kentucky has county income taxes (rates vary)
        { state_code: 'KY', jurisdiction: 'JEFFERSON', tax_type: 'flat', rate: 0.02, fixed_amount: null, brackets_json: null }, // Louisville
        { state_code: 'KY', jurisdiction: 'FAYETTE', tax_type: 'flat', rate: 0.02, fixed_amount: null, brackets_json: null }, // Lexington
        { state_code: 'KY', jurisdiction: 'KENTON', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'BOONE', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'CAMPBELL', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'WARREN', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'HARDIN', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'DAVIESS', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // Owensboro
        { state_code: 'KY', jurisdiction: 'MADISON', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'KY', jurisdiction: 'BULLITT', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },

        // ========== ALABAMA - County Taxes ==========
        // Alabama has county income taxes (rates vary by county)
        { state_code: 'AL', jurisdiction: 'JEFFERSON', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // Birmingham
        { state_code: 'AL', jurisdiction: 'MOBILE', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'MADISON', tax_type: 'flat', rate: 0.005, fixed_amount: null, brackets_json: null }, // Huntsville
        { state_code: 'AL', jurisdiction: 'MONTGOMERY', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'SHELBY', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'TUSCALOOSA', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'BALDWIN', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'LEE', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // Auburn
        { state_code: 'AL', jurisdiction: 'MORGAN', tax_type: 'flat', rate: 0.005, fixed_amount: null, brackets_json: null },
        { state_code: 'AL', jurisdiction: 'CALHOUN', tax_type: 'flat', rate: 0.005, fixed_amount: null, brackets_json: null },

        // ========== COLORADO - Denver ==========
        {
            state_code: 'CO',
            jurisdiction: 'DENVER',
            tax_type: 'flat',
            rate: 0.004, // 0.4% - Denver Occupational Privilege Tax
            fixed_amount: null,
            brackets_json: null
        },

        // ========== MICHIGAN - City Taxes ==========
        // Michigan has city income taxes
        { state_code: 'MI', jurisdiction: 'DETROIT', tax_type: 'flat', rate: 0.024, fixed_amount: null, brackets_json: null }, // 2.4%
        { state_code: 'MI', jurisdiction: 'GRAND_RAPIDS', tax_type: 'flat', rate: 0.015, fixed_amount: null, brackets_json: null }, // 1.5%
        { state_code: 'MI', jurisdiction: 'FLINT', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%
        { state_code: 'MI', jurisdiction: 'LANSING', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%
        { state_code: 'MI', jurisdiction: 'ANN_ARBOR', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%

        // ========== MISSOURI - City Taxes ==========
        // Missouri has city earnings taxes
        { state_code: 'MO', jurisdiction: 'KANSAS_CITY', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%
        { state_code: 'MO', jurisdiction: 'ST_LOUIS', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%

        // ========== OHIO - City Taxes ==========
        // Ohio has city income taxes (in addition to school districts)
        { state_code: 'OH', jurisdiction: 'COLUMBUS', tax_type: 'flat', rate: 0.025, fixed_amount: null, brackets_json: null }, // 2.5%
        { state_code: 'OH', jurisdiction: 'CLEVELAND', tax_type: 'flat', rate: 0.02, fixed_amount: null, brackets_json: null }, // 2%
        { state_code: 'OH', jurisdiction: 'CINCINNATI', tax_type: 'flat', rate: 0.021, fixed_amount: null, brackets_json: null }, // 2.1%
        { state_code: 'OH', jurisdiction: 'TOLEDO', tax_type: 'flat', rate: 0.0225, fixed_amount: null, brackets_json: null }, // 2.25%
        { state_code: 'OH', jurisdiction: 'AKRON', tax_type: 'flat', rate: 0.02, fixed_amount: null, brackets_json: null }, // 2%
        { state_code: 'OH', jurisdiction: 'DAYTON', tax_type: 'flat', rate: 0.025, fixed_amount: null, brackets_json: null }, // 2.5%

        // ========== PENNSYLVANIA - City Taxes ==========
        // Pennsylvania has city wage taxes (in addition to EIT/LST)
        { state_code: 'PA', jurisdiction: 'PHILADELPHIA', tax_type: 'flat', rate: 0.03487, fixed_amount: null, brackets_json: null }, // 3.487%
        { state_code: 'PA', jurisdiction: 'PITTSBURGH', tax_type: 'flat', rate: 0.03, fixed_amount: null, brackets_json: null }, // 3%
        { state_code: 'PA', jurisdiction: 'ERIE', tax_type: 'flat', rate: 0.0165, fixed_amount: null, brackets_json: null }, // 1.65%
        { state_code: 'PA', jurisdiction: 'SCRANTON', tax_type: 'flat', rate: 0.03, fixed_amount: null, brackets_json: null }, // 3%
        { state_code: 'PA', jurisdiction: 'ALLENTOWN', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%

        // ========== OREGON - Transit District Taxes ==========
        { state_code: 'OR', jurisdiction: 'TRI_MET', tax_type: 'flat', rate: 0.007337, fixed_amount: null, brackets_json: null }, // TriMet Transit District
        { state_code: 'OR', jurisdiction: 'LANE_TRANSIT', tax_type: 'flat', rate: 0.007, fixed_amount: null, brackets_json: null }, // Lane Transit District

        // ========== IOWA - School District Surtax ==========
        // Iowa has school district surtax (varies by district)
        { state_code: 'IA', jurisdiction: 'SCHOOL_SURTAX', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // Default 1% - varies

        // ========== NEW JERSEY - City Taxes ==========
        // New Jersey has city payroll taxes
        { state_code: 'NJ', jurisdiction: 'NEWARK', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%
        { state_code: 'NJ', jurisdiction: 'JERSEY_CITY', tax_type: 'flat', rate: 0.01, fixed_amount: null, brackets_json: null }, // 1%
    ];

    return new Promise((resolve, reject) => {
        db.serialize(() => {
            // Clear existing data for 2026
            db.run(`DELETE FROM local_taxes WHERE year = ?`, [year], (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                console.log(`✅ Cleared existing local tax data for ${year}\n`);

                const stmt = db.prepare(`INSERT INTO local_taxes 
                    (year, state_code, jurisdiction, tax_type, rate, fixed_amount, brackets_json) 
                    VALUES (?, ?, ?, ?, ?, ?, ?)`);

                let inserted = 0;
                localTaxes.forEach(tax => {
                    stmt.run(
                        year,
                        tax.state_code,
                        tax.jurisdiction,
                        tax.tax_type,
                        tax.rate,
                        tax.fixed_amount,
                        tax.brackets_json,
                        (err) => {
                            if (err) {
                                console.error(`❌ Error inserting ${tax.state_code}/${tax.jurisdiction}:`, err.message);
                            } else {
                                inserted++;
                            }
                        }
                    );
                });

                stmt.finalize((err) => {
                    if (err) {
                        reject(err);
                    } else {
                        // Wait for all inserts
                        setTimeout(() => {
                            console.log(`\n✅ Imported ${inserted} local tax jurisdictions`);
                            console.log(`\n📊 Summary by State:`);
                            
                            // Count by state
                            const stateCounts = {};
                            localTaxes.forEach(tax => {
                                stateCounts[tax.state_code] = (stateCounts[tax.state_code] || 0) + 1;
                            });
                            
                            Object.keys(stateCounts).sort().forEach(state => {
                                console.log(`   ${state}: ${stateCounts[state]} jurisdictions`);
                            });
                            
                            console.log(`\n📝 Note: Some rates are defaults and may vary by specific jurisdiction.`);
                            console.log(`   Verify actual rates for your specific location.`);
                            
                            resolve();
                        }, 1000);
                    }
                });
            });
        });
    });
}

// Run if called directly
if (require.main === module) {
    importComprehensiveCountyTaxes()
        .then(() => {
            console.log('\n✅ County/local tax import complete');
            process.exit(0);
        })
        .catch(err => {
            console.error('Import error:', err);
            process.exit(1);
        });
}

module.exports = { importComprehensiveCountyTaxes };

