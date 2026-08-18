package f95gm.client.ui.detail.metadata

import f95gm.client.model.detail.DetailMeta
import f95gm.client.model.detail.DetailState
import f95gm.messages.UiMessages

internal fun detailMetaCards(state: DetailState): List<DetailMeta> {
    return state.meta.filterNot { meta ->
        meta.label.value.equals(UiMessages.app_genre(), ignoreCase = true) || isDetailBadgeMeta(meta)
    }
}
