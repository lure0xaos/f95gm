package f95gm.client.state.detail

import f95gm.client.state.runtime.clientController
import f95gm.domain.values.F95ThreadId

internal var loadedDetailId: F95ThreadId?
    get() = clientController.loadedDetailId
    set(value) {
        clientController.loadedDetailId = value
    }
