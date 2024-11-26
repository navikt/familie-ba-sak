package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ba.sak.common.Feil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArbeidsfordelingPåBehandlingRepository : JpaRepository<ArbeidsfordelingPåBehandling, Long> {
    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlingId = :behandlingId")
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling?

    @Query(
        """
        SELECT apb
        FROM ArbeidsfordelingPåBehandling apb
        JOIN Behandling b ON apb.behandlingId = b.id
        WHERE b.fagsak.id = :fagsakId
          AND apb.behandlendeEnhetId != '4863'
          AND b.resultat NOT IN (
                 'HENLAGT_FEILAKTIG_OPPRETTET',
                 'HENLAGT_SØKNAD_TRUKKET',
                 'HENLAGT_AUTOMATISK_FØDSELSHENDELSE',
                 'HENLAGT_TEKNISK_VEDLIKEHOLD'
            )
        ORDER BY b.aktivertTidspunkt DESC
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
