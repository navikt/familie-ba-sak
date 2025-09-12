package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.domene

import org.springframework.data.jpa.repository.JpaRepository

interface SvalbardstilleggKjøringRepository : JpaRepository<SvalbardstilleggKjøring, Long> {
    fun findByFagsakId(fagsakId: Long): SvalbardstilleggKjøring?

    fun findByFagsakIdIn(fagsakIder: Set<Long>): List<SvalbardstilleggKjøring>
}
