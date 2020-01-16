package no.nav.familie.ba.sak.task

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.rest.RestTask
import no.nav.familie.prosessering.rest.RestTaskMapper
import org.springframework.stereotype.Service

@Service
class DefaultRestTaskMapper : RestTaskMapper {
    override fun toDto(task: Task): RestTask {
        return try {
            val taskDTO = objectMapper.convertValue(task, DefaultTaskDTO::class.java)
            RestTask(task, null, null, taskDTO.personIdent)
        } catch (e: IllegalArgumentException) {
            RestTask(task, null, null, "")
        }
    }
}