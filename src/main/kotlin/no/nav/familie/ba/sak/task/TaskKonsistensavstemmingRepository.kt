package no.nav.familie.ba.sak.task

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskKonsistensavstemmingRepository : JpaRepository<Task, String> {

    @Query("select t from task s where s.status != :status and type = :type")
    fun findNotStatusAndType(status: Status, type: String): List<Task>
    fun findByStatusAndType(status: Status, type: String): List<Task>
}
