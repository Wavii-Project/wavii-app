/**
 * URL base de la API de Wavii.
 *
 * Desarrollo temporal: ngrok hacia el backend local.
 * Importante: si levantas ngrok, apunta el tunel a `http://127.0.0.1:8080`
 * y no a `http://localhost:8080` para evitar ambiguedades IPv4/IPv6 en Windows.
 * Si necesitas probar en local sin dominio, cambia temporalmente a:
 *   - Emulador Android : 'http://10.0.2.2:8080'
 *   - Dispositivo fisico: 'http://192.168.X.X:8080'  (IP local del PC)
 */
export const API_BASE_URL = 'https://undeduced-kaden-mentally.ngrok-free.dev';

export const PUBLIC_BASE_URL =
  process.env.EXPO_PUBLIC_PUBLIC_BASE_URL?.trim() || API_BASE_URL;
