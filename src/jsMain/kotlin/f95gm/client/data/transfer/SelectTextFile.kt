package f95gm.client.data.transfer

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

internal fun selectTextFile(onLoaded: (String) -> Unit) {
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
