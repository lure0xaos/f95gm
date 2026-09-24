package f95gm.client.data.transfer

import f95gm.client.state.runtime.BrowserFileTransfer

internal fun selectTextFile(onLoaded: (String) -> Unit) {
    BrowserFileTransfer.selectTextFile(onLoaded)
}
