#!/usr/bin/env node
/**
 * Standalone payroll calculation script
 * Called from Java backend to calculate payroll
 */

const { calculatePaystub } = require('./services/payrollService');

// Read input from stdin
let inputData = '';
process.stdin.setEncoding('utf8');

process.stdin.on('data', (chunk) => {
    inputData += chunk;
});

process.stdin.on('end', () => {
    try {
        const request = JSON.parse(inputData);
        
        calculatePaystub(request)
            .then(result => {
                console.log(JSON.stringify(result));
                process.exit(0);
            })
            .catch(error => {
                console.error(JSON.stringify({ error: error.message, stack: error.stack }));
                process.exit(1);
            });
    } catch (error) {
        console.error(JSON.stringify({ error: error.message }));
        process.exit(1);
    }
});

