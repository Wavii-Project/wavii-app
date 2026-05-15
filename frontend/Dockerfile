FROM node:20-alpine

WORKDIR /app

# Instala Expo CLI
RUN npm install -g @expo/cli@latest

COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps

COPY . .

EXPOSE 8081 19000 19001

# Modo LAN: el móvil y el PC deben estar en la misma red WiFi
# HOST_IP se pasa desde docker-compose (tu IP local)
CMD ["npx", "expo", "start", "--host", "lan", "--port", "8081"]
