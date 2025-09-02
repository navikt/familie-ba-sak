package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService

fun mockTaskService(): TaskService {
    val taskService = mockk<TaskService>()
    every { taskService.save(any()) } answers { firstArg() }
    return taskService
}
