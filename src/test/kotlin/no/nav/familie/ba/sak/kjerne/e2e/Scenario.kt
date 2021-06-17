package no.nav.familie.ba.sak.kjerne.e2e

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.VergeData
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

fun byggE2EMock(mockPersonopplysningerService: PersonopplysningerService, scenario: Scenario): PersonopplysningerService {
    val personMap = mutableMapOf(scenario.søker.personIdent to scenario.søker)
    scenario.barna.forEach { personMap[it.personIdent] = it }

    every {
        mockPersonopplysningerService.hentMaskertPersonInfoVedManglendeTilgang(any())
    } returns null

    every {
        mockPersonopplysningerService.hentAktivAktørId(any())
    } answers {
        randomAktørId()
    }

    every {
        mockPersonopplysningerService.hentAktivPersonIdent(any())
    } answers {
        PersonIdent(randomFnr())
    }

    val identSlot = slot<Ident>()
    every {
        mockPersonopplysningerService.hentIdenter(capture(identSlot))
    } answers {
        listOf(IdentInformasjon(identSlot.captured.ident, false, "FOLKEREGISTERIDENT"))
    }

    every {
        mockPersonopplysningerService.hentDødsfall(any())
    } returns DødsfallData(false, null)

    every {
        mockPersonopplysningerService.hentVergeData(any())
    } returns VergeData(false)

    every {
        mockPersonopplysningerService.hentLandkodeUtenlandskBostedsadresse(any())
    } returns "NO"

    every {
        mockPersonopplysningerService.hentStatsborgerskap(capture(identSlot))
    } answers {
        personMap[identSlot.captured.ident]?.statsborgerskap ?: emptyList()
    }

    every {
        mockPersonopplysningerService.hentOpphold(any())
    } answers {
        listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                       oppholdFra = LocalDate.of(1990, 1, 25),
                       oppholdTil = LocalDate.of(2499, 1, 1)))
    }

    every {
        mockPersonopplysningerService.hentBostedsadresseperioder(any())
    } answers {
        listOf(GrBostedsadresseperiode(
                periode = DatoIntervallEntitet(
                        fom = LocalDate.of(2002, 1, 4),
                        tom = LocalDate.of(2002, 1, 5)
                )))
    }

    every {
        mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(any())
    } answers {
        ADRESSEBESKYTTELSEGRADERING.UGRADERT
    }

    val idSlotForHentPersoninfo = slot<String>()
    every {
        mockPersonopplysningerService.hentPersoninfo(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    every {
        mockPersonopplysningerService.hentHistoriskPersoninfoManuell(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjoner(capture(idSlotForHentPersoninfo))
    } answers {
        personMap[identSlot.captured.ident]?.tilPersonInfo() ?: throw Feil("Finner ikke person")
    }

    return mockPersonopplysningerService
}

fun Scenario.byggRelasjoner(): Scenario {
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

data class Scenario(
        val søker: ScenarioPerson,
        val barna: List<ScenarioPerson>
)

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