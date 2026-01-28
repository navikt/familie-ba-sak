package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import tools.jackson.module.kotlin.readValue

class FakeTaskRepositoryWrapper(
    taskService: TaskService,
) : TaskRepositoryWrapper(taskService) {
    val lagredeTasker = mutableListOf<Task>()

    override fun save(task: Task): Task {
        lagredeTasker.add(task)
        return task
    }

    override fun findAll(): Iterable<Task> = lagredeTasker

    override fun findByStatus(status: Status): List<Task> = lagredeTasker.filter { it.status === status }

    fun hentLagredeTaskerAvType(type: String): List<Task> = this.lagredeTasker.filter { it.type == type }
}

inline fun <reified T> List<Task>.tilPayload(): List<T> = this.map { jsonMapper.readValue(it.payload) }
