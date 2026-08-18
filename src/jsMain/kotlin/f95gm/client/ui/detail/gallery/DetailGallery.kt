package f95gm.client.ui.detail.gallery

import dev.fritz2.core.*
import kotlinx.coroutines.flow.map

internal fun RenderContext.detailGallery() {
    Window.keydownsIf {
        detailGalleryStore.current.images.isNotEmpty() &&
                shortcutOf(this) in setOf(Keys.Escape, Keys.ArrowLeft, Keys.ArrowRight)
    } handledBy { event ->
        when (shortcutOf(event)) {
            Keys.Escape -> closeDetailGallery()
            Keys.ArrowLeft -> previousDetailGalleryImage()
            Keys.ArrowRight -> nextDetailGalleryImage()
        }
    }

    div {
        className(detailGalleryStore.data.map { state ->
            "gallery-lightbox position-fixed top-0 start-0 w-100 h-100 align-items-center justify-content-center bg-dark bg-opacity-75 " +
                    if (state.images.isEmpty()) "d-none" else "d-flex"
        })
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("style", "z-index: 2000;")
        clicks handledBy { closeDetailGallery() }

        div("gallery-stage position-relative d-flex align-items-center justify-content-center w-100 h-100 p-3") {
            clicks { stopPropagation() } handledBy { }

            img("gallery-image mw-100 mh-100 rounded shadow") {
                attr("style", detailGalleryStore.data.map {
                    "max-width: 90vw; max-height: 85vh; transform-origin: center center; " +
                            "transition: transform 120ms ease; transform: scale(${it.scale});"
                })
                src(detailGalleryStore.data.map { it.current?.source.orEmpty() })
                alt(detailGalleryStore.data.map { it.current?.alt.orEmpty() })
            }

            div("gallery-controls position-absolute top-0 end-0 d-flex align-items-center gap-2 p-3") {
                span("gallery-counter badge text-bg-dark") {
                    detailGalleryStore.data.map { state ->
                        if (state.current == null) "" else "${state.index + 1} / ${state.images.size}"
                    }.render { +it }
                }
                galleryButton("−", "Zoom out", ::zoomOutDetailGallery)
                galleryButton("+", "Zoom in", ::zoomInDetailGallery)
                galleryButton("×", "Close", ::closeDetailGallery)
            }

            galleryButton(
                "‹", "Previous image", ::previousDetailGalleryImage,
                "position-absolute top-50 start-0 translate-middle-y ms-3"
            ) {
                hidden(detailGalleryStore.data.map { it.images.size < 2 })
            }
            galleryButton(
                "›", "Next image", ::nextDetailGalleryImage,
                "position-absolute top-50 end-0 translate-middle-y me-3"
            ) {
                hidden(detailGalleryStore.data.map { it.images.size < 2 })
            }
        }
    }
}
