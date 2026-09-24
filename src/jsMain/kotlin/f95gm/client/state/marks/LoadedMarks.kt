package f95gm.client.state.marks

import f95gm.client.state.runtime.clientController

internal var loadedMarks
    get() = clientController.areMarksLoaded()
    set(value) {
        clientController.setMarksLoaded(value)
    }
