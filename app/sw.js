// 固定缓存名：更新不再依赖版本号；导航优先快速返回，静态资源 stale-while-revalidate
const CACHE = 'qianliao';
const ASSETS = ['./', './index.html', './manifest.json', './icon-192.png', './icon-512.png', './vendor/katex/katex.min.js', './vendor/katex/katex.min.css'];
const NAV_TIMEOUT = 900;

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ASSETS)).then(() => self.skipWaiting()));
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
      return (await caches.match('./index.html')) || (await caches.match('./')) || Response.error();
    })());
    return;
  }

  e.respondWith((async () => {
    const hit = await caches.match(req);
    if (hit) { fresh(req).catch(() => {}); return hit; }
    return (await fresh(req).catch(() => null)) || (await caches.match('./index.html')) || Response.error();
  })());
});
