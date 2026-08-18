package f95gm.client.ui.detail.metadata

import f95gm.client.model.detail.DetailMeta
import f95gm.client.model.detail.DetailState

internal fun detailBadgeMeta(state: DetailState): List<DetailMeta> = state.meta.filter(::isDetailBadgeMeta)
