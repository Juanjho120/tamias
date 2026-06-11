const fs = require('fs');
const path = require('path');

const apiBaseUrl = process.env.FRONTEND_API_BASE_URL;

if (!apiBaseUrl || !apiBaseUrl.trim()) {
  console.error('[write-prod-env] FRONTEND_API_BASE_URL is required.');
  console.error('[write-prod-env] Examples:');
  console.error('[write-prod-env]   FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1');
  console.error('[write-prod-env]   FRONTEND_API_BASE_URL=/api/v1');
  process.exit(1);
}

const normalizedApiBaseUrl = apiBaseUrl.trim().replace(/\/$/, '');

const isAbsoluteUrl = /^https?:\/\//i.test(normalizedApiBaseUrl);
const isRelativeApiPath = normalizedApiBaseUrl.startsWith('/');

if (!isAbsoluteUrl && !isRelativeApiPath) {
  console.error(`[write-prod-env] FRONTEND_API_BASE_URL must be an absolute URL or a relative path starting with "/": ${apiBaseUrl}`);
  console.error('[write-prod-env] Valid examples:');
  console.error('[write-prod-env]   https://tamias-api-testing.onrender.com/api/v1');
  console.error('[write-prod-env]   /api/v1');
  process.exit(1);
}

if (isAbsoluteUrl) {
  try {
    const parsedUrl = new URL(normalizedApiBaseUrl);

    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
      console.error('[write-prod-env] FRONTEND_API_BASE_URL must start with http:// or https://');
      process.exit(1);
    }
  } catch (_error) {
    console.error(`[write-prod-env] FRONTEND_API_BASE_URL is not a valid URL: ${apiBaseUrl}`);
    process.exit(1);
  }
}

const escapedApiBaseUrl = normalizedApiBaseUrl.replace(/\\/g, '\\\\').replace(/'/g, "\\'");

const environmentFilePath = path.resolve(__dirname, '../src/environments/environment.prod.ts');

const fileContent = `export const environment = {
  production: true,
  apiBaseUrl: '${escapedApiBaseUrl}'
};
`;

fs.writeFileSync(environmentFilePath, fileContent, 'utf8');

console.log(`[write-prod-env] environment.prod.ts generated with apiBaseUrl=${normalizedApiBaseUrl}`);
