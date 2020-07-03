package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.nare.core.specifications.Spesifikasjon


enum class Filtreringsregler(val spesifikasjon: Spesifikasjon<Fakta>) {

    MOR_HAR_GYLDIG_FOEDSELSNUMMER(Spesifikasjon(
            "Mor har gyldig fødselsnummer",
            "MOR_HAR_GYLDIG_FOEDSELSNUMMER",
            implementasjon = { morHarGyldigFødselsnummer(this) })
    ),
    BARNET_HAR_GYLDIG_FOEDSELSNUMMER(Spesifikasjon(
            "Barnet har gyldig fødselsnummer",
            "BARNET_HAR_GYLDIG_FOEDSELSNUMMER",
            implementasjon = { barnetHarGyldigFødselsnummer(this) })
    ),
    BARNET_ER_UNDER_6_MND(Spesifikasjon(
            "Barnet er under 6 måneder",
            "BARNET_ER_UNDER_6_MND",
            implementasjon = { barnetErUnder6mnd(this) })
    ),
    MOR_ER_OVER_18_AAR(Spesifikasjon(
            "Mor er over 18 år",
            "MOR_ER_OVER_18_AAR",
            implementasjon = { morErOver18år(this) })
    ),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(Spesifikasjon(
            "Det har gått mer enn 5 måneder siden forrige barn ble født",
            "MER_ENN_5_MND_SIDEN_FORRIGE_BARN",
            implementasjon = { merEnn5mndSidenForrigeBarn(this) })
    ),
    MOR_LEVER(Spesifikasjon(
            "Mor lever",
            "MOR_LEVER",
            implementasjon = { morLever(this) })
    ),
    BARNET_LEVER(Spesifikasjon(
            "Barnet lever",
            "BARNET_LEVER",
            implementasjon = { barnetLever(this) })
    ),
    MOR_HAR_IKKE_VERGE(Spesifikasjon(
            "Mor har ikke verge",
            "MOR_HAR_IKKE_VERGE",
            implementasjon = { morHarIkkeVerge(this) })
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

