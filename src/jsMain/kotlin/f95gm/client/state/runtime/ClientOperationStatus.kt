package f95gm.client.state.runtime

/** Single async lifecycle model shared by feature data and keyed client operations. */
internal typealias ClientOperationStatus = AsyncState<Unit>
