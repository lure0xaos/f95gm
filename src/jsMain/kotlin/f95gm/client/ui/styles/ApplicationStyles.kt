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
}
