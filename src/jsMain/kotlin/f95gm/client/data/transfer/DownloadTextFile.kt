package f95gm.client.data.transfer

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

internal fun downloadTextFile(fileName: String, content: String) {
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
