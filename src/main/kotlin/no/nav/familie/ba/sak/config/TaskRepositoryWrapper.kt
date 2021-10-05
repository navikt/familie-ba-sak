package no.nav.familie.ba.sak.config

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/*
TaskRepository in familie-prosessering is @Primary, which is not able to mock so we use this wrapper class for testibility
 */
@Profile("!mock-task-repository")
@Component
class TaskRepositoryWrapper(private val taskRepository: TaskRepository) {

    fun save(task: Task) =
        taskRepository.save(task)

    fun findAll(): Iterable<Task> =
        taskRepository.findAll()

    fun findByStatus(status: Status): List<Task> = taskRepository.findByStatus(status)
}
