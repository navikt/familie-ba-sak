package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.nare.Spesifikasjon


/**
 * Rekkefølgen på reglene er vesentlig, og brukes når man finner riktig begrunnelse
 * i forbindelse med opprettelse av oppgave.
 */
enum class Filtreringsregler(val spesifikasjon: Spesifikasjon<Fakta>) {

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
            "Det har gått mer enn 5 måneder eller mindre enn 5 dager siden forrige barn ble født",
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

