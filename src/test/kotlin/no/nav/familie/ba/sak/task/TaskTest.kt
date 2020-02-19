package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.stream.Collectors

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class TaskTest {

    @Autowired
    lateinit var tasker: List<AsyncTaskStep>

    @Test
    fun `Tasker skal ha unikt navn`() {
        val taskTyper: List<String> = tasker.stream()
                .map { task: AsyncTaskStep ->
                    finnAnnotasjon(task)
                }
                .map { it?.taskStepType }
                .collect(
                        Collectors.toList<String>())


        Assertions.assertEquals(tasker.size, taskTyper.distinct().size)
    }

    @Test
    fun `Tasker skal ha annotasjon`() {
        Assertions.assertEquals(false, tasker.stream().anyMatch {
            harIkkePåkrevdAnnotasjon(
                    it!!)
        })
    }

    private fun harIkkePåkrevdAnnotasjon(it: AsyncTaskStep): Boolean {
        return !AnnotationUtils.isAnnotationDeclaredLocally(TaskStepBeskrivelse::class.java,
                                                            it.javaClass)
    }

    private fun finnAnnotasjon(task: AsyncTaskStep): TaskStepBeskrivelse? {
        val aClass = AopProxyUtils.ultimateTargetClass(task)
        return AnnotationUtils.findAnnotation(aClass,
                                              TaskStepBeskrivelse::class.java)
    }
}