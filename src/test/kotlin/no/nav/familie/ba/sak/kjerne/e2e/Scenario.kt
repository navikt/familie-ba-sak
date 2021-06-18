package no.nav.familie.ba.sak.kjerne.e2e

import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

data class Scenario(
        val søker: ScenarioPerson,
        val barna: List<ScenarioPerson>
) {

    fun byggRelasjoner(): Scenario {
        return this.copy(
                søker = søker.copy(
                        familierelasjoner = barna.map {
                            Familierelasjon(relatertPersonsIdent = it.personIdent,
                                            relatertPersonsRolle = FORELDERBARNRELASJONROLLE.BARN,
                                            fødselsdato = it.fødselsdato,
                                            navn = it.navn)
                        }
                ),
                barna = barna.map {
                    it.copy(
                            familierelasjoner = listOf(Familierelasjon(relatertPersonsIdent = it.personIdent,
                                                                       relatertPersonsRolle = FORELDERBARNRELASJONROLLE.MOR,
                                                                       fødselsdato = it.fødselsdato,
                                                                       navn = it.navn))
                    )
                }
        )
    }
}

data class ScenarioPerson(
        val personIdent: String = randomFnr(),
        val aktørId: String = randomAktørId().id,
        val familierelasjoner: List<Familierelasjon> = emptyList(),
        val fødselsdato: LocalDate,
        val fornavn: String,
        val etternavn: String,
        val kjønn: Kjønn = Kjønn.KVINNE,
        val statsborgerskap: List<Statsborgerskap> = listOf(Statsborgerskap(
                land = "NO",
                gyldigFraOgMed = fødselsdato,
                gyldigTilOgMed = null
        )),
) {

    val navn = "$fornavn $etternavn"
}

val defaultBostedsadresseHistorikk = mutableListOf(
        Bostedsadresse(angittFlyttedato = LocalDate.now().minusDays(15),
                       gyldigTilOgMed = null,
                       matrikkeladresse = Matrikkeladresse(matrikkelId = 123L,
                                                           bruksenhetsnummer = "H301",
                                                           tilleggsnavn = "navn",
                                                           postnummer = "0202",
                                                           kommunenummer = "2231")),
        Bostedsadresse(angittFlyttedato = LocalDate.now().minusYears(1),
                       gyldigTilOgMed = LocalDate.now().minusDays(16),
                       matrikkeladresse = Matrikkeladresse(matrikkelId = 123L,
                                                           bruksenhetsnummer = "H301",
                                                           tilleggsnavn = "navn",
                                                           postnummer = "0202",
                                                           kommunenummer = "2231"))
)

val defaultSivilstandHistorisk = listOf(
        Sivilstand(type = SIVILSTAND.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
        Sivilstand(type = SIVILSTAND.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
)

fun ScenarioPerson.tilPersonInfo() = PersonInfo(fødselsdato = this.fødselsdato,
                                                bostedsadresser = defaultBostedsadresseHistorikk,
                                                kjønn = this.kjønn,
                                                navn = this.navn,
                                                sivilstander = defaultSivilstandHistorisk,
                                                statsborgerskap = this.statsborgerskap,
                                                forelderBarnRelasjon = this.familierelasjoner.map {
                                                    ForelderBarnRelasjon(personIdent = Personident(id = it.relatertPersonsIdent),
                                                                         relasjonsrolle = it.relatertPersonsRolle,
                                                                         navn = it.navn,
                                                                         fødselsdato = it.fødselsdato)
                                                }.toSet())

data class Familierelasjon(
        val relatertPersonsIdent: String,
        val relatertPersonsRolle: FORELDERBARNRELASJONROLLE,
        val navn: String,
        val fødselsdato: LocalDate
)