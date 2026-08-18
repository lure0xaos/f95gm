package f95gm.client.ui.detail.metadata

import f95gm.client.model.detail.DetailMeta
import f95gm.messages.UiMessages

internal fun isDetailBadgeMeta(meta: DetailMeta): Boolean {
    val label = meta.label.value.trim()
    return label.equals(UiMessages.app_version(), ignoreCase = true) || label.equals("engine", ignoreCase = true)
}
