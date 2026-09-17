// Kotlin's webpack config lives in build/js/packages/f95gm.
if (config.entry && config.entry.main) {
    config.entry.main.unshift(require('path').resolve(__dirname, '../../../../webpack/style-nonce.js'));
}
