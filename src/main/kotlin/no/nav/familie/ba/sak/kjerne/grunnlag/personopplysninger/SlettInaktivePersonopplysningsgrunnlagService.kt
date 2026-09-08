package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SlettInaktivePersonopplysningsgrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    /**
     * Sletter én batch inaktive personopplysningsgrunnlag (id større enn [etterId], stigende).
     * Tilhørende po_person og registeropplysninger slettes av ON DELETE CASCADE i databasen.
     *
     * Hver batch kjøres i egen transaksjon slik at låser og WAL holdes små, og allerede slettede
     * batcher ikke rulles tilbake om en senere batch feiler.
     *
     * @return id-ene som ble slettet, stigende (tom liste når det ikke er flere)
     */
    @Transactional
    fun slettBatchMedInaktiveGrunnlag(
        etterId: Long,
        batchStørrelse: Int,
    ): List<Long> {
        val grunnlagIder =
            personopplysningGrunnlagRepository.finnIderForInaktiveGrunnlagMedAktivtGrunnlagPåSammeBehandling(
                etterId = etterId,
                pageable = PageRequest.of(0, batchStørrelse),
            )
        if (grunnlagIder.isEmpty()) return emptyList()

        personopplysningGrunnlagRepository.slettPersonopplysningsgrunnlag(grunnlagIder)
        return grunnlagIder
    }

    fun tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling(): Long = personopplysningGrunnlagRepository.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()
}
