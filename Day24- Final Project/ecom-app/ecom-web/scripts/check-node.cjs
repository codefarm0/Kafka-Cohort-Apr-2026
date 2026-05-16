/**
 * Fails fast before Vite runs (avoids cryptic ERR_UNSUPPORTED_ESM_URL_SCHEME on old Node).
 */
const match = /^v(\d+)\./.exec(process.version);
const major = match ? parseInt(match[1], 10) : 0;
if (major < 18) {
  console.error(`
[ecom-web] Node.js 18+ is required (20 LTS recommended).
            Current: ${process.version}

            Upgrade: https://nodejs.org  or  nvm install 20 && nvm use
`);
  process.exit(1);
}
