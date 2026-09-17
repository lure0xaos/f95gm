import {defineConfig} from '@playwright/test';

export default defineConfig({
    testDir: '.',
    timeout: 30_000,
    reporter: [['list']],
    webServer: [
        {
            command: 'node mock-upstream.mjs',
            ignoreHTTPSErrors: true,
            url: `${process.env.F95GM_MOCK_UPSTREAM_URL ?? 'https://localhost:18081'}/health`,
            timeout: 30_000,
            reuseExistingServer: false,
        },
        {
            command: process.platform === 'win32'
                ? `cmd /c "${process.env.F95GM_E2E_COMMAND}"`
                : process.env.F95GM_E2E_COMMAND,
            url: `${process.env.F95GM_E2E_URL ?? 'http://127.0.0.1:18080'}/api/health?tabId=browser-e2e`,
            timeout: 30_000,
            reuseExistingServer: false,
        },
    ],
    use: {
        baseURL: process.env.F95GM_E2E_URL ?? 'http://127.0.0.1:18080',
        browserName: 'chromium',
        headless: true,
    },
});
