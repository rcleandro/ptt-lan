package com.pttlan.shared

import kotlinx.coroutines.Job

/** What Swift holds while it watches a Kotlin flow; [cancel] it when the view that watches goes away. */
class Observation internal constructor(
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
    }
}
