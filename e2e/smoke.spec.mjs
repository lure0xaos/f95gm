import {expect, test} from '@playwright/test';

const fixture = scenario => ({
    valid: {login: 'fixture-user', password: 'fixture-password'},
    invalid: {login: 'fixture-user', password: 'wrong-password'},
    malformed: {login: 'fixture-malformed', password: 'fixture-password'},
    unavailable: {login: 'fixture-unavailable', password: 'fixture-password'},
}[scenario]);

test('loads the packaged browser shell and reaches the server health endpoint', async ({page}) => {
    const pageErrors = [];
    page.on('pageerror', error => pageErrors.push(error));
    const health = await page.request.get('/api/health?tabId=browser-e2e');
    expect(health.ok()).toBeTruthy();

    const shellResponse = await page.goto('/');
    const styleNonce = /'nonce-([^']+)'/.exec(shellResponse.headers()['content-security-policy'])[1];
    await expect(page).toHaveTitle('F95 Game Manager');
    await expect(page.locator('body')).toHaveClass(/bg-black/);
    await expect(page.locator('input[name="login"]')).toBeVisible();
    await expect(page.locator('input[name="login"]')).toHaveAttribute('autocomplete', 'username');
    await expect(page.locator('input[name="password"]')).toHaveAttribute('autocomplete', 'current-password');
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect.poll(() => page.locator('.app-shell').evaluate(element => getComputedStyle(element).backgroundColor))
        .toBe('rgb(0, 0, 0)');
    expect(await page.locator('style').evaluateAll((styles, nonce) =>
        styles.length > 0 && styles.every(style => style.nonce === nonce), styleNonce
    )).toBeTruthy();
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

test('returns a safe localized failure for invalid upstream credentials', async ({page}) => {
    await page.goto('/');
    await page.locator('input[name="login"]').fill('fixture-user');
    await page.locator('input[name="password"]').fill('wrong-password');
    const response = await page.request.post('/api/login', {
        data: fixture('invalid'),
        headers: {Accept: 'application/json'},
    });
    expect(response.status()).toBe(401);
    const body = await response.json();
    expect(body.code ?? 'INTERNAL').toBe('INTERNAL');
    expect(body.message).toMatch(/credentials|incorrect/i);
    expect((await page.context().cookies()).some(cookie => cookie.name === 'f95gm_session')).toBe(false);
});

test('returns a safe failure when the upstream responds with malformed authentication content', async ({page}) => {
    await page.goto('/');
    const response = await page.request.post('/api/login', {
        data: fixture('malformed'),
        headers: {Accept: 'application/json'},
    });
    expect(response.status()).toBe(401);
    expect((await page.context().cookies()).some(cookie => cookie.name === 'f95gm_session')).toBe(false);
});

test('handles an unavailable upstream response without creating a session', async ({page}) => {
    await page.goto('/');
    const response = await page.request.post('/api/login', {
        data: fixture('unavailable'),
        headers: {Accept: 'application/json'},
    });
    expect(response.status()).toBe(401);
    expect((await page.context().cookies()).some(cookie => cookie.name === 'f95gm_session')).toBe(false);
});

test('authenticates, restores, loads the catalog, signs out, and rejects an invalidated session', async ({page}) => {
    const valid = fixture('valid');
    await page.goto('/');
    await page.locator('input[name="login"]').fill(valid.login);
    await page.locator('input[name="password"]').fill(valid.password);
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

    await page.locator('input[name="login"]').fill(valid.login);
    await page.locator('input[name="password"]').fill(valid.password);
    const secondLogin = await page.request.post('/api/login', {
        data: valid,
        headers: {Accept: 'application/json'},
    });
    expect(secondLogin.status()).toBe(200);
    await page.reload();
    await expect(page.getByRole('button', {name: /sign out/i})).toBeVisible();
    const cookie = (await page.context().cookies()).find(item => item.name === 'f95gm_session');
    expect(cookie).toBeTruthy();
    await page.context().addCookies([{...cookie, value: '00000000-0000-0000-0000-000000000099'}]);
    await page.reload();
    await expect(page.locator('input[name="login"]')).toBeVisible();
    expect((await page.context().cookies()).some(item => item.name === 'f95gm_session')).toBe(false);
});


test('catalog filter sections keep their layout and apply selected values', async ({page}, testInfo) => {
    await page.goto('/');
    await page.locator('input[name="login"]').fill(fixture('valid').login);
    await page.locator('input[name="password"]').fill(fixture('valid').password);
    const optionsLoaded = page.waitForResponse(response => new URL(response.url()).pathname === '/api/catalog/options');
    await page.getByRole('button', {name: /open catalog/i}).click();
    await expect(page.getByRole('button', {name: /sign out/i})).toBeVisible();
    const optionsResponse = await optionsLoaded;
    expect(optionsResponse.status()).toBe(200);
    expect((await optionsResponse.json()).prefixes).toHaveLength(1);
    await page.goto('/#/latest-updates/');
    await expect(page.getByText('E2E Fixture Game')).toBeVisible();
    await page.locator('.filter-toggle').click();
    const panel = page.locator('#catalog-filter-panel');
    await expect(panel).toBeVisible();
    const cards = panel.locator('.filter-card');
    await expect(cards).toHaveCount(4);
    await expect(panel.locator('.filter-card > .filter-title')).toHaveText([
        'SEARCH', 'TIME & TAG MATCHING', 'TAGS', 'Engine',
    ]);
    for (const [name, width] of [['desktop', 1440], ['mobile', 390]]) {
        await page.setViewportSize({width, height: 1000});
        await expect(panel.getByPlaceholder('Search developers\u2026')).toBeVisible();
        const boxes = await cards.evaluateAll(elements => elements.map(element => {
            const {x, width} = element.getBoundingClientRect();
            return {x, width};
        }));
        expect(boxes.every(box => box.width > 0 && box.width <= width)).toBeTruthy();
        if (name === 'desktop') expect(boxes[1].x).toBeGreaterThan(boxes[0].x);
        else expect(Math.abs(boxes[1].x - boxes[0].x)).toBeLessThan(1);
        await panel.screenshot({path: testInfo.outputPath(`filters-${name}.png`)});
    }
    await page.setViewportSize({width: 1440, height: 1000});
    await panel.getByPlaceholder('Search developers\u2026').fill('Fixture Studio');
    const searchCard = cards.nth(0);
    await searchCard.locator('.dropdown-button').click();
    await searchCard.getByRole('button', {name: 'Developers', exact: true}).click();
    const matchingCard = cards.nth(1);
    await matchingCard.locator('.dropdown-button').nth(0).click();
    await matchingCard.getByRole('button', {name: 'Today', exact: true}).click();
    await matchingCard.locator('.dropdown-button').nth(1).click();
    await matchingCard.getByRole('button', {name: 'Match every selected tag (AND)', exact: true}).click();
    const tags = cards.nth(2).locator('.filter-picker').first();
    await tags.locator('.dropdown-button').click();
    await tags.getByRole('button', {name: 'Adventure', exact: true}).click();
    await expect(tags.getByRole('button', {name: 'Adventure', exact: true})).toHaveClass(/active/);
    await tags.locator('.dropdown-button').click();
    const prefixes = cards.nth(3).locator('.filter-picker').first();
    await prefixes.locator('.dropdown-button').click();
    await prefixes.getByRole('button', {name: 'RenPy', exact: true}).click();
    await expect(prefixes.getByRole('button', {name: 'RenPy', exact: true})).toHaveClass(/active/);
    await prefixes.locator('.dropdown-button').click();
    const applied = page.waitForRequest(request => {
        const url = new URL(request.url());
        return url.pathname.endsWith('/catalog');
    });
    await panel.getByRole('button', {name: /apply filters/i}).click();
    const params = new URL((await applied).url()).searchParams;
    expect(params.get('creator')).toBe('Fixture Studio');
    expect(params.get('date')).toBe('1');
    expect(params.get('tagtype')).toBe('and');
    expect(params.get('tags')).toContain('11');
    expect(params.get('prefixes')).toContain('21');
    await panel.getByRole('button', {name: /clear/i}).click();
    await expect(panel.getByPlaceholder('Search developers\u2026')).toHaveValue('');
    await expect(searchCard.locator('.dropdown-button')).toHaveText('Titles');
});
