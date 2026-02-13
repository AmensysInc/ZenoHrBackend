/**
 * Strict table validation for production
 * Validates all required tables exist and have data
 * Fails fast if any critical table is missing
 */

const { getDatabase } = require('./init');

// Required tables for payroll calculations
const REQUIRED_TABLES = {
    // Critical - must have data
    critical: [
        { name: 'tax_years', description: 'Tax year definitions', minRows: 1 },
        { name: 'fica_rates', description: 'FICA rates (Social Security, Medicare)', minRows: 1, yearFilter: true },
        { name: 'federal_deductions', description: 'Federal standard deductions', minRows: 1, yearFilter: true },
        { name: 'pub15t_percentage_tables', description: 'IRS Pub 15-T percentage tables', minRows: 100, yearFilter: true }
    ],
    // Important - should have data but can fallback
    important: [
        { name: 'federal_brackets', description: 'Federal tax brackets', minRows: 1, yearFilter: true },
        { name: 'state_brackets', description: 'State tax brackets', minRows: 0, yearFilter: true },
        { name: 'state_deductions', description: 'State standard deductions', minRows: 0, yearFilter: true },
        { name: 'state_withholding_tables', description: 'State withholding tables', minRows: 0, yearFilter: true },
        { name: 'state_payroll_taxes', description: 'State payroll taxes', minRows: 0, yearFilter: true },
        { name: 'local_taxes', description: 'Local tax rates', minRows: 0 }
    ]
};

/**
 * Validate all required tables
 * @param {number} taxYear - Tax year to validate (default: 2026)
 * @returns {Promise<{valid: boolean, errors: string[], warnings: string[]}>}
 */
function validateTables(taxYear = 2026) {
    return new Promise((resolve, reject) => {
        const db = getDatabase();
        const errors = [];
        const warnings = [];
        
        let checksCompleted = 0;
        const totalChecks = REQUIRED_TABLES.critical.length + REQUIRED_TABLES.important.length;
        
        function checkComplete() {
            checksCompleted++;
            if (checksCompleted === totalChecks) {
                const valid = errors.length === 0;
                resolve({ valid, errors, warnings });
            }
        }
        
        // Validate critical tables
        REQUIRED_TABLES.critical.forEach(table => {
            const yearFilter = table.yearFilter ? ` WHERE year = ${taxYear}` : '';
            db.get(`SELECT COUNT(*) as count FROM ${table.name}${yearFilter}`, (err, row) => {
                if (err) {
                    errors.push(`CRITICAL: ${table.description} (${table.name}) - Table error: ${err.message}`);
                } else if (!row || row.count < table.minRows) {
                    errors.push(`CRITICAL: ${table.description} (${table.name}) - Missing data (found ${row.count || 0}, required ${table.minRows})`);
                }
                checkComplete();
            });
        });
        
        // Validate important tables
        REQUIRED_TABLES.important.forEach(table => {
            const yearFilter = table.yearFilter ? ` WHERE year = ${taxYear}` : '';
            db.get(`SELECT COUNT(*) as count FROM ${table.name}${yearFilter}`, (err, row) => {
                if (err) {
                    warnings.push(`IMPORTANT: ${table.description} (${table.name}) - Table error: ${err.message}`);
                } else if (!row || row.count < table.minRows) {
                    warnings.push(`IMPORTANT: ${table.description} (${table.name}) - Limited data (found ${row.count || 0}, recommended ${table.minRows})`);
                }
                checkComplete();
            });
        });
    });
}

/**
 * Get detailed table status
 * @param {number} taxYear - Tax year to check (default: 2026)
 * @returns {Promise<Object>}
 */
function getTableStatus(taxYear = 2026) {
    return new Promise((resolve, reject) => {
        const db = getDatabase();
        const status = {
            taxYear,
            critical: {},
            important: {},
            summary: { total: 0, valid: 0, missing: 0, errors: 0 }
        };
        
        let checksCompleted = 0;
        const allTables = [...REQUIRED_TABLES.critical, ...REQUIRED_TABLES.important];
        const totalChecks = allTables.length;
        
        function checkComplete() {
            checksCompleted++;
            if (checksCompleted === totalChecks) {
                // Calculate summary
                Object.values(status.critical).forEach(s => {
                    status.summary.total++;
                    if (s.valid) status.summary.valid++;
                    else if (s.error) status.summary.errors++;
                    else status.summary.missing++;
                });
                Object.values(status.important).forEach(s => {
                    status.summary.total++;
                    if (s.valid) status.summary.valid++;
                    else if (s.error) status.summary.errors++;
                    else status.summary.missing++;
                });
                resolve(status);
            }
        }
        
        allTables.forEach(table => {
            const yearFilter = table.yearFilter ? ` WHERE year = ${taxYear}` : '';
            const category = REQUIRED_TABLES.critical.includes(table) ? 'critical' : 'important';
            
            db.get(`SELECT COUNT(*) as count FROM ${table.name}${yearFilter}`, (err, row) => {
                const tableStatus = {
                    name: table.name,
                    description: table.description,
                    valid: false,
                    count: 0,
                    error: null
                };
                
                if (err) {
                    tableStatus.error = err.message;
                } else {
                    tableStatus.count = row ? row.count : 0;
                    tableStatus.valid = tableStatus.count >= table.minRows;
                }
                
                status[category][table.name] = tableStatus;
                checkComplete();
            });
        });
    });
}

module.exports = {
    validateTables,
    getTableStatus,
    REQUIRED_TABLES
};

