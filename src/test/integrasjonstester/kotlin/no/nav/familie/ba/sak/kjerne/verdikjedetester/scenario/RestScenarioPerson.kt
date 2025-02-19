package no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario

import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate
import java.time.Month

data class RestScenarioPerson(
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
    val bostedsadresser: List<Bostedsadresse> = defaultBostedsadresseHistorikk,
) {
    val ident: String
        get() = _ident ?: randomFnr(LocalDate.parse(fødselsdato)).also { _ident = it }

    val aktørId: String
        get() = _ident + "99"
    val navn = "$fornavn $etternavn"
}

val defaultBostedsadresseHistorikk =
    mutableListOf(
        Bostedsadresse(
            angittFlyttedato = LocalDate.now().minusDays(15),
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
        Bostedsadresse(
            angittFlyttedato = LocalDate.of(2018, Month.JANUARY, 1),
            gyldigTilOgMed = LocalDate.now().minusDays(16),
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
