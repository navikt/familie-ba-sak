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
     * @return id-ene som ble slettet, stigende (tom liste når det ikke er flere), og antall slettede rader per tabell
     */
    @Transactional
    fun slettBatchMedInaktiveGrunnlag(
        etterId: Long,
        batchStørrelse: Int,
    ): SlettetBatch {
        val grunnlagIder =
            personopplysningGrunnlagRepository.finnIderForInaktiveGrunnlagMedAktivtGrunnlagPåSammeBehandling(
                etterId = etterId,
                pageable = PageRequest.of(0, batchStørrelse),
            )
        if (grunnlagIder.isEmpty()) return SlettetBatch(grunnlagIder = emptyList(), antallSlettedeRaderPerTabell = emptyMap())

        val antallRaderSomSlettesViaCascade = personopplysningGrunnlagRepository.tellRaderSomSlettesMedGrunnlag(grunnlagIder)
        val antallSlettedeGrunnlag = personopplysningGrunnlagRepository.slettPersonopplysningsgrunnlag(grunnlagIder)

        val antallSlettedeRaderPerTabell =
            buildMap {
                put("gr_personopplysninger", antallSlettedeGrunnlag.toLong())
                antallRaderSomSlettesViaCascade.forEach { put(it.tabell, it.antall) }
            }
        return SlettetBatch(grunnlagIder = grunnlagIder, antallSlettedeRaderPerTabell = antallSlettedeRaderPerTabell)
    }

    fun tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling(): Long = personopplysningGrunnlagRepository.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()
}

data class SlettetBatch(
    val grunnlagIder: List<Long>,
    val antallSlettedeRaderPerTabell: Map<String, Long>,
)
