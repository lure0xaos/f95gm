package f95gm.client.ui.detail

import dev.fritz2.core.RenderContext
import f95gm.client.model.detail.DetailSection
import f95gm.client.ui.detail.metadata.detailCardMetaLabels
import f95gm.client.ui.shared.content.richHtml
import f95gm.messages.UiMessages

internal fun RenderContext.renderDetailBody(
    overviewHtml: String,
    sections: List<DetailSection>,
    fallbackHtml: String
) {
    val richSections = sections.filterNot { section ->
        detailCardMetaLabels.any { it.equals(section.title.value, ignoreCase = true) }
    }
    if (overviewHtml.isNotBlank()) {
        div("overview-card card border-primary-subtle mb-3") {
            detailCardHeader(UiMessages.app_overview(), "detail-overview", "bi bi-card-text me-2")
            richHtml("overview-body collapse card-body detail-rich-content", overviewHtml, "detail-overview")
        }
    }

    if (richSections.isNotEmpty()) {
        div("section-grid row row-cols-1 row-cols-lg-2 g-3") {
            richSections.forEachIndexed { index, section ->
                val bodyId = "detail-section-$index"
                div("section-column col") {
                    div("section-card card h-100") {
                        detailCardHeader(section.title.value, bodyId)
                        richHtml("section-body collapse card-body detail-rich-content", section.html.value, bodyId)
                    }
                }
            }
        }
    }

    if (overviewHtml.isBlank() && richSections.isEmpty() && fallbackHtml.isNotBlank()) {
        richHtml("fallback-body detail-rich-content", fallbackHtml)
    }
}
