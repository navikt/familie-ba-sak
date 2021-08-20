package no.nav.familie.ba.sak.task

import io.mockk.mockk
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class TaskRepositoryTestConfig {

    @Bean
    @Profile("mock-task-repository")
    fun mockTaskRepository(): TaskRepository {

        return mockk(relaxed = true)
    }

    @Bean
    @Profile("mock-task-service")
    @Primary
    fun mockTaskService(): OpprettTaskService {

        return mockk(relaxed = true)
    }
}