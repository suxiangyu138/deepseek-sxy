// 固定缓存名：更新不再依赖版本号；导航优先快速返回，静态资源 stale-while-revalidate
const CACHE = 'qianliao';
const ASSETS = ['./', './index.html', './manifest.json', 'icon-192.png?v=1.9.5', 'icon-512.png?v=1.9.5', './vendor/katex/katex.min.js', './vendor/katex/katex.min.css'];
const NAV_TIMEOUT = 900;

self.addEventListener('install', (e) => {
  e.waitUntil((async () => {
    const box = await caches.open(CACHE);
    for (const u of ASSETS) { try { await box.add(u); } catch (err) {} }
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

async function fresh(req) {
  const res = await fetch(req, { cache: 'no-cache' });
  if (res && res.ok) {
    const box = await caches.open(CACHE);
    box.put(req, res.clone()).catch(() => {});
  }
  return res;
}

function offlinePage() {
  return new Response('<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
    + '<body style="font:15px -apple-system,sans-serif;background:#f5f5f7;color:#1d1d1f;display:flex;align-items:center;justify-content:center;height:100vh;margin:0">'
    + '<div style="text-align:center"><p style="font-size:16px;margin:0 0 8px">本地服务没有启动</p>'
    + '<p style="color:#86868b;font-size:12px;margin:0 0 18px">打开 dsh-go 后再点重试</p>'
    + '<button onclick="location.reload()" style="padding:9px 20px;border-radius:999px;border:1px solid #d0d0d5;background:#fff;font-size:13px">重试</button></div></body>', {headers:{'Content-Type':'text/html; charset=utf-8'}});
}

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== location.origin) return; // API 直连，不缓存

  if (req.mode === 'navigate') {
    // 网络最多等 900ms，超时直接给缓存；后台继续拉新版本，下次打开生效
    e.respondWith((async () => {
      const net = fresh(req).catch(() => null);
      const timeout = new Promise((resolve) => setTimeout(() => resolve(null), NAV_TIMEOUT));
      const first = await Promise.race([net, timeout]);
      if (first) return first;
      return (await caches.match('./index.html')) || (await caches.match('./')) || offlinePage();
    })());
    return;
  }

  e.respondWith((async () => {
    const hit = await caches.match(req);
    if (hit) { fresh(req).catch(() => {}); return hit; }
    return (await fresh(req).catch(() => null)) || (await caches.match('./index.html')) || Response.error();
  })());
});
