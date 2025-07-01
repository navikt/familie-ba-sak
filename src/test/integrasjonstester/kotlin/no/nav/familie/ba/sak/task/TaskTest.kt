package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import java.util.stream.Collectors

@Tag("integration")
class TaskTest : AbstractSpringIntegrationTest() {
    @Autowired
    lateinit var tasker: List<AsyncTaskStep>

    @Test
    fun `Tasker skal ha unikt navn`() {
        val taskTyper: List<String> =
            tasker
                .stream()
                .map { task: AsyncTaskStep ->
                    finnAnnotasjon(task)
                }.map { it?.taskStepType }
                .collect(
                    Collectors.toList<String>(),
                )

        Assertions.assertEquals(tasker.size, taskTyper.distinct().size)
    }

    @Test
    fun `Tasker skal ha annotasjon`() {
        Assertions.assertEquals(
            false,
            tasker.stream().anyMatch {
                harIkkePåkrevdAnnotasjon(
                    it!!,
                )
            },
        )
    }

    @Test
    fun `doTask skal ha annotasjon WithSpan for bedre tracing`() {
        val taskerUtenWithSpan =
            tasker.mapNotNull { task ->
                val doTaskMethod =
                    task.javaClass.declaredMethods.find { method ->
                        method.name == "doTask"
                    }
                if (doTaskMethod == null || !doTaskMethod.isAnnotationPresent(WithSpan::class.java)) {
                    "${task::class.java.name}.doTask() mangler @WithSpan"
                } else {
                    null
                }
            }

        assertTrue(taskerUtenWithSpan.isEmpty(), taskerUtenWithSpan.joinToString("\n"))
    }

    private fun harIkkePåkrevdAnnotasjon(it: AsyncTaskStep): Boolean =
        !AnnotationUtils.isAnnotationDeclaredLocally(
            TaskStepBeskrivelse::class.java,
            it.javaClass,
        )

    private fun finnAnnotasjon(task: AsyncTaskStep): TaskStepBeskrivelse? {
        val aClass = AopProxyUtils.ultimateTargetClass(task)
        return AnnotationUtils.findAnnotation(
            aClass,
            TaskStepBeskrivelse::class.java,
        )
    }
}
