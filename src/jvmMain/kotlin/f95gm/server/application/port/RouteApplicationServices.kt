package f95gm.server.application.port


/** Application capability port consumed by route composition. */
internal interface RouteApplicationServices {
    val marks: MarkApplicationPort
    val savedFilters: SavedFilterApplicationPort
    val mutations: IdempotencyApplicationPort
}
