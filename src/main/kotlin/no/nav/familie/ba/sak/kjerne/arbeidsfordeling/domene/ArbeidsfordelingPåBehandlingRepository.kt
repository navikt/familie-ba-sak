package no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ba.sak.common.Feil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArbeidsfordelingPåBehandlingRepository : JpaRepository<ArbeidsfordelingPåBehandling, Long> {
    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlingId = :behandlingId")
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling?
}

// Extension-function fordi default methods for JPA ikke er støttet uten @JvmDefaultWithCompatibility
fun ArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling = finnArbeidsfordelingPåBehandling(behandlingId) ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling $behandlingId")
