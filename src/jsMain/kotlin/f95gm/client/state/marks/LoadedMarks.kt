package f95gm.client.state.marks

import f95gm.client.state.runtime.clientController

internal var loadedMarks
    get() = clientController.marksLoaded
    set(value) {
        clientController.marksLoaded = value
    }
