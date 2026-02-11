# Paystub PDF Service

A Node.js service using Puppeteer to generate PDFs from HTML content.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Start the service:
```bash
npm start
```

The service will run on port 3002 by default.

## API

### POST /generate-pdf

Generates a PDF from HTML content.

**Request Body:**
```json
{
  "html": "<html>...</html>",
  "options": {
    "format": "Letter",
    "margin": { "top": "0", "right": "0", "bottom": "0", "left": "0" }
  }
}
```

**Response:**
- Content-Type: `application/pdf`
- Returns PDF file as binary data

## Environment Variables

- `PORT`: Server port (default: 3002)

