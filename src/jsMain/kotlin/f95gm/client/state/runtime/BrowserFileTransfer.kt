package f95gm.client.state.runtime

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader

internal interface BrowserFileTransferPort {
    fun selectTextFile(onLoaded: (String) -> Unit)
    fun downloadTextFile(fileName: String, content: String)
}

internal object BrowserFileTransfer {
    internal var port: BrowserFileTransferPort = DomBrowserFileTransfer

    fun selectTextFile(onLoaded: (String) -> Unit) = port.selectTextFile(onLoaded)
    fun downloadTextFile(fileName: String, content: String) = port.downloadTextFile(fileName, content)
}

private object DomBrowserFileTransfer : BrowserFileTransferPort {
    override fun selectTextFile(onLoaded: (String) -> Unit) {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = ".json,application/json"
        input.hidden = true
        input.onchange = {
            input.files?.item(0)?.let { file ->
                FileReader().apply {
                    onload = {
                        onLoaded(result?.toString().orEmpty())
                        input.remove()
                    }
                    readAsText(file)
                }
            }
            null
        }
        document.body?.appendChild(input)
        input.click()
    }

    override fun downloadTextFile(fileName: String, content: String) {
        val blob = Blob(arrayOf(content), BlobPropertyBag(type = "application/json"))
        val url = URL.createObjectURL(blob)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = url
        link.download = fileName
        document.body?.appendChild(link)
        link.click()
        link.remove()
        URL.revokeObjectURL(url)
    }
}
