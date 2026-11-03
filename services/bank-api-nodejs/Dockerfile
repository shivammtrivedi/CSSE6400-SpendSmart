FROM node:20-alpine
WORKDIR /app
COPY services/bank-api-nodejs/package*.json ./
RUN apk add --no-cache python3 make g++
RUN npm ci --only=production
COPY services/bank-api-nodejs/ .
EXPOSE 3000
CMD ["node", "index.js"]
