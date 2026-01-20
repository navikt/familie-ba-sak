package no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario

import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

data class ScenarioPersonDto(
    private var _ident: String? = null,
    // yyyy-mm-dd
    val fødselsdato: String,
    val fornavn: String,
    val etternavn: String,
    val statsborgerskap: List<Statsborgerskap> =
        listOf(
            Statsborgerskap(
                land = "NOR",
                gyldigFraOgMed = LocalDate.parse(fødselsdato),
                bekreftelsesdato = LocalDate.parse(fødselsdato),
                gyldigTilOgMed = null,
            ),
        ),
    val oppholdstillatelse: List<Opphold> =
        listOf(
            Opphold(
                type = OPPHOLDSTILLATELSE.PERMANENT,
                oppholdFra = LocalDate.now().minusYears(10),
                oppholdTil = null,
            ),
        ),
    val bostedsadresser: List<Bostedsadresse> = defaultBostedsadresseHistorikk(LocalDate.parse(fødselsdato)),
) {
    val ident: String
        get() = _ident ?: randomFnr(LocalDate.parse(fødselsdato)).also { _ident = it }

    val aktørId: String
        get() = _ident + "99"
    val navn = "$fornavn $etternavn"
}

fun defaultBostedsadresseHistorikk(fødselsdato: LocalDate) =
    mutableListOf(
        Bostedsadresse(
            angittFlyttedato = fødselsdato,
            gyldigFraOgMed = fødselsdato,
            gyldigTilOgMed = null,
            matrikkeladresse =
                Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
        ),
    )

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: String,
)
