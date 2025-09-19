package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene

import org.springframework.data.jpa.repository.JpaRepository

interface SvalbardtilleggKjøringRepository : JpaRepository<SvalbardtilleggKjøring, Long> {
    fun findByFagsakId(fagsakId: Long): SvalbardtilleggKjøring?

    fun findByFagsakIdIn(fagsakIder: Set<Long>): List<SvalbardtilleggKjøring>
}
