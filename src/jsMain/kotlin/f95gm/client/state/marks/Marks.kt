package f95gm.client.state.marks

import dev.fritz2.core.RootStore
import f95gm.client.model.marks.MarksState
import kotlinx.coroutines.Job

internal val marks = RootStore(MarksState(), Job())
