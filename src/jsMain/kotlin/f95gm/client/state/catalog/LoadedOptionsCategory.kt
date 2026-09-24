package f95gm.client.state.catalog

import f95gm.client.state.runtime.clientController
import f95gm.domain.filters.F95Category

internal var loadedOptionsCategory: F95Category?
    get() = clientController.loadedOptionsCategory
    set(value) {
        clientController.loadedOptionsCategory = value
    }
