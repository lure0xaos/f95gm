package f95gm.client.data.transfer

import f95gm.client.state.runtime.BrowserFileTransfer

internal fun downloadTextFile(fileName: String, content: String) {
    BrowserFileTransfer.downloadTextFile(fileName, content)
}
