// Run before CSS imports so style-loader uses the server's per-response CSP nonce.
const globalStyles = document.getElementById('fritz2-global-styles');
if (globalStyles && globalStyles.nonce) {
    __webpack_nonce__ = globalStyles.nonce;
}
