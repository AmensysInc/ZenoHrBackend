FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (including dev dependencies for database scripts)
RUN npm ci

# Copy application files
COPY . .

# Create database directory if it doesn't exist
RUN mkdir -p /app/database

# Expose port
EXPOSE 3000

# Start the service (server.js will initialize database on startup)
CMD ["node", "server.js"]

