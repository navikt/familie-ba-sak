package no.nav.familie.ba.sak.journalføring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import javax.persistence.LockModeType

interface JournalføringRepository: JpaRepository<DbJournalpost, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(dbJournalpost: DbJournalpost): DbJournalpost
}