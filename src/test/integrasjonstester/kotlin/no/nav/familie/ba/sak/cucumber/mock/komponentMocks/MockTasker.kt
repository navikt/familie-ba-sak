package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.core.annotation.AnnotationUtils

class MockTasker {
    val tasker = mutableListOf<Pair<AsyncTaskStep, Task>>()

    // Sørger for at taskene blir kjørt i riktig rekkefølge og at vi ikke starter på en ny task før den forrige er ferdig.
    fun kjørTaskOmIkkeAnnenTaskKjører(
        taskStep: AsyncTaskStep,
        task: Task,
    ) {
        tasker.add(taskStep to task)
        if (tasker.size == 1) {
            kjørNesteTask()
        }
    }

    fun kjørNesteTask() {
        if (tasker.isNotEmpty()) {
            val (taskStep, task) = tasker.first()
            taskStep.doTask(task)
            taskStep.onCompletion(task)
            tasker.removeAt(0)
            if (tasker.isNotEmpty()) {
                kjørNesteTask()
            }
        }
    }

    fun mockTaskRepositoryWrapper(
        cucumberMock: CucumberMock,
        scope: CoroutineScope?,
    ): TaskRepositoryWrapper {
        val taskRepositoryWrapper = mockk<TaskRepositoryWrapper>()
        every { taskRepositoryWrapper.save(any()) } answers {
            val task = firstArg<Task>()
            val tasktyper =
                cucumberMock.taskservices
                    .map { it }
                    .map { asynctaskstep ->
                        val aClass = AopProxyUtils.ultimateTargetClass(asynctaskstep)
                        val annotation = AnnotationUtils.findAnnotation(aClass, TaskStepBeskrivelse::class.java)
                        requireNotNull(annotation) { "annotasjon mangler" }
                        annotation.taskStepType to asynctaskstep
                    }.toMap()

            val asyncTaskStep = tasktyper[task.type]
            if (asyncTaskStep != null) {
                if (scope != null) {
                    scope.launch {
                        kjørTaskOmIkkeAnnenTaskKjører(asyncTaskStep, task)
                    }
                } else {
                    kjørTaskOmIkkeAnnenTaskKjører(asyncTaskStep, task)
                }
            } else {
                logger.warn("Fant ikke taskstep for tasktype ${task.type} i cucumberMock. Kjører ikke tasken.")
            }

            task
        }
        return taskRepositoryWrapper
    }
}
