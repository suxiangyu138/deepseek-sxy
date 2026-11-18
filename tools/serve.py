#!/usr/bin/env python3
import http.server, socketserver, os, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
PORT = int(sys.argv[2]) if len(sys.argv) > 2 else 8899

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Access-Control-Allow-Origin', '*')
        super().end_headers()

    def guess_type(self, path):
        if path.endswith('.apk'):
            return 'application/vnd.android.package-archive'
        if path.endswith('.webmanifest') or path.endswith('.json'):
            return 'application/json'
        return super().guess_type(path)

class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True

with Server(('0.0.0.0', PORT), Handler) as httpd:
    httpd.serve_forever()
