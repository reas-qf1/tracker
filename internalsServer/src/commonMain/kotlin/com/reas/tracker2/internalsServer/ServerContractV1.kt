package com.reas.tracker2.internalsServer

import com.reas.tracker2.shared.RawEvent
import com.reas.tracker2.shared.Scrobble

interface ServerContractV1 {
    suspend fun submit(events: List<RawEvent>): List<Scrobble>
}
