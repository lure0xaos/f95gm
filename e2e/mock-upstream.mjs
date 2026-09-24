import {createServer} from 'node:http';

const port = Number(process.env.F95GM_MOCK_UPSTREAM_PORT ?? 18081);
const catalog = JSON.stringify({
    status: 'ok',
    msg: {
        data: [{
            thread_id: '987654',
            title: 'E2E Fixture Game',
            version: '1.0',
            developer: 'Fixture Studio',
            date: 'Today'
        }], total: 1
    },
});
const latestPage = `<!doctype html><html><body>Welcome, fixture-user<script>var latestUpdates = {"tags":{},"prefixes":{"games":[]}};</script></body></html>`;

const server = createServer(async (request, response) => {
    const url = new URL(request.url ?? '/', `http://127.0.0.1:${port}`);
    if (url.pathname === '/health') {
        response.writeHead(200, {'content-type': 'text/plain'}).end('ok');
        return;
    }
    if (url.pathname === '/login/' && request.method === 'GET') {
        response.writeHead(200, {'content-type': 'text/html; charset=utf-8'})
            .end('<!doctype html><html><body><input name="_xfToken" value="fixture-token"></body></html>');
        return;
    }
    if (url.pathname === '/login/login' && request.method === 'POST') {
        let body = '';
        for await (const chunk of request) body += chunk;
        const form = new URLSearchParams(body);
        if (form.get('login') !== 'fixture-user' || form.get('password') !== 'fixture-password' || form.get('_xfToken') !== 'fixture-token') {
            response.writeHead(200, {'content-type': 'text/html; charset=utf-8'}).end('<html><body><div class="blockMessage--error">Incorrect password</div></body></html>');
            return;
        }
        response.writeHead(303, {
            location: '/sam/latest_alpha/',
            'set-cookie': 'xf_session=fixture-session; Path=/; HttpOnly; SameSite=Lax',
        }).end();
        return;
    }
    if (url.pathname === '/sam/latest_alpha/' && request.method === 'GET') {
        if (request.headers.cookie?.includes('xf_session=fixture-session')) {
            response.writeHead(200, {'content-type': 'text/html; charset=utf-8'}).end(latestPage);
        } else {
            response.writeHead(200, {'content-type': 'text/html; charset=utf-8'})
                .end('<html><body>You have to be logged in to access this page</body></html>');
        }
        return;
    }
    if (url.pathname === '/sam/latest_alpha/latest_data.php' && request.method === 'GET') {
        if (!request.headers.cookie?.includes('xf_session=fixture-session')) {
            response.writeHead(401, {'content-type': 'application/json'}).end('{"status":"error","msg":"login required"}');
            return;
        }
        response.writeHead(200, {'content-type': 'application/json; charset=utf-8'}).end(catalog);
        return;
    }
    response.writeHead(404, {'content-type': 'text/plain'}).end('fixture route not found');
});

server.listen(port, '127.0.0.1');
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.close(() => process.exit(0)));
