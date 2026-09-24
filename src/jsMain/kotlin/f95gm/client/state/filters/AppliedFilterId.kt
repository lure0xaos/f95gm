package f95gm.client.state.filters

import f95gm.client.state.runtime.clientController
import f95gm.domain.values.F95FilterId

internal var appliedFilterId: F95FilterId?
    get() = clientController.appliedFilterId
    set(value) {
        clientController.appliedFilterId = value
    }
