package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.nare.core.specifications.Spesifikasjon

// Rekkefølgen på reglene er vesentlig, og brukes når man finner riktig begrunnelse
// i forbindelse med opprettelse av oppgave.
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
    BARNET_LEVER(Spesifikasjon(
            "Barnet lever",
            "BARNET_LEVER",
            implementasjon = { barnetLever(this) })
    ),
    MOR_LEVER(Spesifikasjon(
            "Mor lever",
            "MOR_LEVER",
            implementasjon = { morLever(this) })
    ),
    MOR_ER_OVER_18_AAR(Spesifikasjon(
            "Mor er over 18 år",
            "MOR_ER_OVER_18_AAR",
            implementasjon = { morErOver18år(this) })
    ),
    MOR_HAR_IKKE_VERGE(Spesifikasjon(
            "Mor har ikke verge",
            "MOR_HAR_IKKE_VERGE",
            implementasjon = { morHarIkkeVerge(this) })
    ),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(Spesifikasjon(
            "Det har gått mer enn 5 måneder siden forrige barn ble født",
            "MER_ENN_5_MND_SIDEN_FORRIGE_BARN",
            implementasjon = { merEnn5mndSidenForrigeBarn(this) })
    ),
    BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING(Spesifikasjon(
            "Saken medfører etterbetaling",
            "BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING",
            implementasjon = { barnetsFødselsdatoInnebærerIkkeEtterbetaling(this)})
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

