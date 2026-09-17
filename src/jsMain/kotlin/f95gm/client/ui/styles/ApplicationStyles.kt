package f95gm.client.ui.styles

internal val applicationStyles = cssRules {
    ".login-page" {
        "background-image"("linear-gradient(90deg, rgba(0, 0, 0, .98) 0%, rgba(0, 0, 0, .86) 39%, rgba(0, 0, 0, .10) 72%), url('girl.jpg')")
        "background-position"("right center")
        "background-size"("auto 100%")
        "background-repeat"("no-repeat")
    }
    ".game-card" {
        "transition"("border-color 150ms ease-in-out")
    }
    ".game-card:hover, .game-card:focus-within" {
        "border-color"("white !important")
    }
    ".game-link, .my-game-card-link, .detail-header" {
        "cursor"("pointer")
    }
    ".page-loading-overlay, .gallery-lightbox" {
        "z-index"("2000")
    }
    ".page-loading-overlay" {
        "background"("rgba(13, 17, 23, 0.78)")
    }
    ".page-input" {
        "width"("4rem")
    }
    ".dropdown-menu-options" {
        "max-height"("min(18rem, calc(100vh - 8rem))")
    }
    ".jvm-connection-indicator" {
        "width"("0.65rem")
        "height"("0.65rem")
    }
    ".gallery-image" {
        "max-width"("90vw")
        "max-height"("85vh")
        "transform-origin"("center center")
        "transition"("transform 120ms ease")
    }
    for (zoom in 4..30 step 2) {
        ".gallery-image-scale-$zoom" {
            "transform"("scale(${zoom / 10.0})")
        }
    }
}
