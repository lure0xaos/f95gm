import {expect, test} from '@playwright/test';

test('loads the packaged browser shell and reaches the server health endpoint', async ({page}) => {
    const pageErrors = [];
    page.on('pageerror', error => pageErrors.push(error));
    const health = await page.request.get('/api/health?tabId=browser-e2e');
    expect(health.ok()).toBeTruthy();

    await page.goto('/');
    await expect(page).toHaveTitle('F95 Game Manager');
    await expect(page.locator('body')).toHaveClass(/bg-black/);
    await expect(page.locator('input[name="login"]')).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.locator('img:not([alt])')).toHaveCount(0);
    expect(await page.locator('button:visible').evaluateAll(buttons => buttons.every(button =>
        Boolean(button.getAttribute('aria-label') || button.textContent?.trim())
    ))).toBeTruthy();
    expect(pageErrors).toEqual([]);
});

test('rejects an unauthenticated catalog request using the browser cookie context', async ({page}) => {
    await page.goto('/');
    expect([401, 403]).toContain(await page.evaluate(() => fetch('/api/catalog').then(response => response.status)));
});

test('authenticates, restores, loads the catalog, signs out, and rejects an invalidated session', async ({page}) => {
    await page.goto('/');
    await page.locator('input[name="login"]').fill('fixture-user');
    await page.locator('input[name="password"]').fill('fixture-password');
    await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/login') && response.status() === 200),
        page.getByRole('button', {name: /open catalog/i}).click(),
    ]);
    await expect(page.getByRole('button', {name: /sign out/i})).toBeVisible();
    const sessionCookie = (await page.context().cookies()).find(cookie => cookie.name === 'f95gm_session');
    expect(sessionCookie?.httpOnly).toBe(true);
    expect(sessionCookie?.sameSite).toBe('Lax');
    const csrfResponse = await page.request.post('/api/logout', {
        headers: {Origin: 'https://evil.example', Accept: 'application/json'},
        data: {},
    });
    expect(csrfResponse.status()).toBe(403);
    expect(await page.evaluate(() => fetch('/api/session').then(response => response.status))).toBe(200);

    await page.goto('/#/latest-updates/');
    await expect(page.getByText('E2E Fixture Game')).toBeVisible();
    await page.reload();
    await expect(page.getByRole('button', {name: /sign out/i})).toBeVisible();
    expect(await page.evaluate(() => fetch('/api/session').then(response => response.status))).toBe(200);

    await page.getByRole('button', {name: /sign out/i}).click();
    await expect(page.locator('input[name="login"]')).toBeVisible();
    expect((await page.context().cookies()).some(cookie => cookie.name === 'f95gm_session')).toBe(false);

    await page.locator('input[name="login"]').fill('fixture-user');
    await page.locator('input[name="password"]').fill('fixture-password');
    await page.getByRole('button', {name: /open catalog/i}).click();
    await expect(page.getByRole('button', {name: /sign out/i})).toBeVisible();
    const cookie = (await page.context().cookies()).find(item => item.name === 'f95gm_session');
    expect(cookie).toBeTruthy();
    await page.context().addCookies([{...cookie, value: '00000000-0000-0000-0000-000000000099'}]);
    await page.reload();
    await expect(page.locator('input[name="login"]')).toBeVisible();
    expect((await page.context().cookies()).some(item => item.name === 'f95gm_session')).toBe(false);
});
