package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ba.sak.common.Feil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArbeidsfordelingPåBehandlingRepository : JpaRepository<ArbeidsfordelingPåBehandling, Long> {
    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlingId = :behandlingId")
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling?

    @Query(
        """
        SELECT apb.*
        FROM arbeidsfordeling_pa_behandling apb
            JOIN behandling b ON apb.fk_behandling_id = b.id
        WHERE b.fk_fagsak_id = :fagsakId
          AND apb.behandlende_enhet_id != '4863'
          AND b.status = 'AVSLUTTET'
          AND b.resultat NOT IN (
             'HENLAGT_FEILAKTIG_OPPRETTET',
             'HENLAGT_SØKNAD_TRUKKET',
             'HENLAGT_AUTOMATISK_FØDSELSHENDELSE',
             'HENLAGT_TEKNISK_VEDLIKEHOLD'
            )   
        ORDER BY b.aktivert_tid DESC
        LIMIT 1
        """,
        nativeQuery = true,
    )
    fun finnSisteGyldigeArbeidsfordelingPåBehandlingIFagsak(
        fagsakId: Long,
    ): ArbeidsfordelingPåBehandling?
}

// Extension-function fordi default methods for JPA ikke er støttet uten @JvmDefaultWithCompatibility
fun ArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling = finnArbeidsfordelingPåBehandling(behandlingId) ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling $behandlingId")
