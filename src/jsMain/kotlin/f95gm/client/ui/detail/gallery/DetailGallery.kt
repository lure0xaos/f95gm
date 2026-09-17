package f95gm.client.ui.detail.gallery

import dev.fritz2.core.*
import kotlinx.coroutines.flow.map

import f95gm.client.state.runtime.ClientGalleryUiPort

internal fun RenderContext.detailGallery(runtime: ClientGalleryUiPort) {
    Window.keydownsIf {
        runtime.detailGalleryView.current.images.isNotEmpty() &&
                shortcutOf(this) in setOf(Keys.Escape, Keys.ArrowLeft, Keys.ArrowRight)
    } handledBy { event ->
        when (shortcutOf(event)) {
            Keys.Escape -> runtime.galleryCommands.closeDetailGallery()
            Keys.ArrowLeft -> runtime.galleryCommands.previousDetailGalleryImage()
            Keys.ArrowRight -> runtime.galleryCommands.nextDetailGalleryImage()
        }
    }

    div {
        className(runtime.detailGalleryView.data.map { state ->
            "gallery-lightbox position-fixed top-0 start-0 w-100 h-100 align-items-center justify-content-center bg-dark bg-opacity-75 " +
                    if (state.images.isEmpty()) "d-none" else "d-flex"
        })
        attr("role", "dialog")
        attr("aria-modal", "true")
        clicks handledBy { runtime.galleryCommands.closeDetailGallery() }

        div("gallery-stage position-relative d-flex align-items-center justify-content-center w-100 h-100 p-3") {
            clicks { stopPropagation() } handledBy { }

            img("gallery-image mw-100 mh-100 rounded shadow") {
                className(runtime.detailGalleryView.data.map { "gallery-image-scale-${(it.scale * 10).toInt()}" })
                src(runtime.detailGalleryView.data.map { it.current?.source?.value.orEmpty() })
                alt(runtime.detailGalleryView.data.map { it.current?.alt.orEmpty() })
            }

            div("gallery-controls position-absolute top-0 end-0 d-flex align-items-center gap-2 p-3") {
                span("gallery-counter badge text-bg-dark") {
                    runtime.detailGalleryView.data.map { state ->
                        if (state.current == null) "" else "${state.index + 1} / ${state.images.size}"
                    }.render { +it }
                }
                galleryButton("−", "Zoom out", { runtime.galleryCommands.zoomOutDetailGallery() })
                galleryButton("+", "Zoom in", { runtime.galleryCommands.zoomInDetailGallery() })
                galleryButton("×", "Close", { runtime.galleryCommands.closeDetailGallery() })
            }

            galleryButton(
                "‹", "Previous image", { runtime.galleryCommands.previousDetailGalleryImage() },
                "position-absolute top-50 start-0 translate-middle-y ms-3"
            ) {
                hidden(runtime.detailGalleryView.data.map { it.images.size < 2 })
            }
            galleryButton(
                "›", "Next image", { runtime.galleryCommands.nextDetailGalleryImage() },
                "position-absolute top-50 end-0 translate-middle-y me-3"
            ) {
                hidden(runtime.detailGalleryView.data.map { it.images.size < 2 })
            }
        }
    }
}
