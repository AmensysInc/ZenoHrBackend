const express = require('express');
const puppeteer = require('puppeteer');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
const PORT = process.env.PORT || 3002;

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: '10mb' }));
app.use(bodyParser.urlencoded({ extended: true, limit: '10mb' }));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'paystub-pdf-service' });
});

// PDF generation endpoint
app.post('/generate-pdf', async (req, res) => {
  let browser = null;
  try {
    const { html, options = {} } = req.body;

    if (!html) {
      return res.status(400).json({ error: 'HTML content is required' });
    }

    console.log('Starting PDF generation...');
    console.log('HTML length:', html.length, 'characters');
    
    // Launch browser
    browser = await puppeteer.launch({
      headless: 'new',
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-accelerated-2d-canvas',
        '--disable-gpu',
        '--disable-web-security'
      ]
    });

    const page = await browser.newPage();

    // Set viewport to match letter size
    await page.setViewport({
      width: 816, // 8.5in * 96 DPI
      height: 1056, // 11in * 96 DPI
      deviceScaleFactor: 2
    });

    // Set content
    await page.setContent(html, {
      waitUntil: ['load', 'networkidle0'],
      timeout: 30000
    });

    // Wait for fonts to load
    await page.evaluateHandle(() => document.fonts.ready);

    // Additional wait for any animations or dynamic content
    await page.waitForTimeout(500);

    // Generate PDF
    const pdfOptions = {
      format: 'Letter',
      printBackground: true,
      margin: {
        top: '0',
        right: '0',
        bottom: '0',
        left: '0'
      },
      preferCSSPageSize: true,
      ...options
    };

    const pdfBuffer = await page.pdf(pdfOptions);

    console.log('PDF generated successfully, size:', pdfBuffer.length, 'bytes');

    // Set response headers
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'attachment; filename="paystub.pdf"');
    res.setHeader('Content-Length', pdfBuffer.length);

    res.send(pdfBuffer);
  } catch (error) {
    console.error('Error generating PDF:', error);
    console.error('Error stack:', error.stack);
    res.status(500).json({ 
      error: 'Failed to generate PDF', 
      message: error.message,
      stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
    });
  } finally {
    if (browser) {
      await browser.close();
    }
  }
});

app.listen(PORT, () => {
  console.log(`PDF Service running on port ${PORT}`);
});

