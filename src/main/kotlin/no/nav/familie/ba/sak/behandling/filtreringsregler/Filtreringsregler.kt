package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.nare.core.specifications.Spesifikasjon


enum class Filtreringsregler(val spesifikasjon: Spesifikasjon<Fakta>) {

    SØKER_HAR_GYLDIG_FØDSELSNUMMER(Spesifikasjon(
            "Søker har gyldig fødselsnummer",
            "SØKER_HAR_GYLDIG_FØDSELSNUMMER",
            implementasjon = { søkerHarGyldigFødselsnummer(this) })
    ),
    BARNET_HAR_GYLDIG_FØDSELSNUMMER(Spesifikasjon(
            "Barnet har gyldig fødselsnummer",
            "BARNET_HAR_GYLDIG_FØDSELSNUMMER",
            implementasjon = { barnetHarGyldigFødselsnummer(this) })
    ),
    BARNET_ER_UNDER_6_MND(Spesifikasjon(
            "Barnet er under 6 måneder",
            "BARNET_ER_UNDER_6_MND",
            implementasjon = { barnetErUnder6mnd(this) })
    ),
    SØKER_ER_OVER_18_ÅR(Spesifikasjon(
            "Søker er over 18 år",
            "SØKER_ER_OVER_18_ÅR",
            implementasjon = { søkerErOver18år(this) })
    );
    override fun toString(): String {
        return this.spesifikasjon.beskrivelse
    }
    companion object {

        fun hentSamletSpesifikasjon(): Spesifikasjon<Fakta> {
            return values().toSet()
                    .map { filtreringsregler -> filtreringsregler.spesifikasjon }
                    .reduce { samledeFiltreringsregler, filtreringsregler -> samledeFiltreringsregler og filtreringsregler }
        }
    }
}

