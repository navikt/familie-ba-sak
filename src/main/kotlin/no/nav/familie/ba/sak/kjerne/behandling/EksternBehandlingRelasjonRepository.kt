package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EksternBehandlingRelasjonRepository : JpaRepository<EksternBehandlingRelasjon, Long> {
    @Query("SELECT ebr FROM EksternBehandlingRelasjon ebr where ebr.internBehandlingId = :internBehandlingId")
    fun findAllByInternBehandlingId(internBehandlingId: Long): List<EksternBehandlingRelasjon>

    @Query("SELECT ebr FROM EksternBehandlingRelasjon ebr where ebr.internBehandlingId = :internBehandlingId and ebr.eksternBehandlingFagsystem = :fagsystem")
    fun findByInternBehandlingIdOgFagsystem(
        internBehandlingId: Long,
        fagsystem: EksternBehandlingRelasjon.Fagsystem,
    ): EksternBehandlingRelasjon?
}
