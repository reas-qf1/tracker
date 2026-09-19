package com.reas.tracker2.ui.viewmodels

import com.reas.tracker2.database.Repository
import com.reas.tracker2.util.InMemoryLog
import kotlinx.coroutines.flow.runningFold

class DebugScreenViewModel(
    private val repository: Repository,
    private val inMemoryLog: InMemoryLog,
) : TrackerViewModel() {
    val playCount
        get() = repository.getPlayCount().asIntStateFlow()

    val mediaEventLog
        get() =
            inMemoryLog["MediaEvents"]
                .runningFold("") { a, b -> "$a$b\n" }
                .asStringStateFlow()
}
