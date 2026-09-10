import { cp, mkdir, rm } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('.', import.meta.url));
const dist = join(root, 'dist');
const contentTypes = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.json': 'application/json; charset=utf-8', '.svg': 'image/svg+xml' };
await rm(dist, { recursive: true, force: true });
await mkdir(dist, { recursive: true });
await cp(join(root, 'index.html'), join(dist, 'index.html'));
await cp(join(root, 'src'), join(dist, 'src'), { recursive: true });
console.log(`Built java-heap-workbench → ${dist}`);

if (process.argv.includes('--serve')) {
  const port = Number(process.env.PORT || 4173);
  createServer(async (req, res) => {
    const path = req.url === '/' ? '/index.html' : req.url;
    const file = join(dist, path.replace(/^\//, '').split('?')[0]);
    try {
      const body = await (await import('node:fs/promises')).readFile(file);
      res.writeHead(200, { 'Content-Type': contentTypes[extname(file)] || 'application/octet-stream' });
      res.end(body);
    } catch { res.writeHead(404); res.end('Not found'); }
  }).listen(port, () => console.log(`Workbench at http://127.0.0.1:${port}`));
}
