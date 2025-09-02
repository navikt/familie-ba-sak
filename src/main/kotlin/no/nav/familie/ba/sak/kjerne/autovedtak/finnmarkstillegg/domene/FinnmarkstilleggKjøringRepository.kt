package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene

import org.springframework.data.jpa.repository.JpaRepository

interface FinnmarkstilleggKjøringRepository : JpaRepository<FinnmarkstilleggKjøring, Long> {
    fun findByFagsakId(fagsakId: Long): FinnmarkstilleggKjøring?
}
