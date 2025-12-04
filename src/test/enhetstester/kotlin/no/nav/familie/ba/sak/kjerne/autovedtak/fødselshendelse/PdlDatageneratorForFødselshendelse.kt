package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrMatrikkeladresseBostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

val konstantAdresse: List<Bostedsadresse> =
    listOf(
        Bostedsadresse(
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            vegadresse =
                Vegadresse(
                    matrikkelId = 6367230663,
                    husnummer = "36",
                    husbokstav = "D",
                    bruksenhetsnummer = null,
                    adressenavn = "Arnulv Eide -veien",
                    kommunenummer = "5422",
                    tilleggsnavn = null,
                    postnummer = "9050",
                ),
        ),
    )

val alternaltivAdresse: List<Bostedsadresse> =
    listOf(
        Bostedsadresse(
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            vegadresse =
                Vegadresse(
                    matrikkelId = 1111000000,
                    husnummer = "36",
                    husbokstav = "D",
                    bruksenhetsnummer = null,
                    adressenavn = "IkkeSamme-veien",
                    kommunenummer = "5423",
                    tilleggsnavn = null,
                    postnummer = "9050",
                ),
            matrikkeladresse = null,
            ukjentBosted = null,
        ),
    )

val mockBarnAutomatiskBehandlingFnr = "21131777001"
val mockBarnAutomatiskBehandling =
    PersonInfo(
        fødselsdato = LocalDate.now(),
        navn = "ARTIG MIDTPUNKT",
        kjønn = Kjønn.KVINNE,
        forelderBarnRelasjon = emptySet(),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = konstantAdresse,
        sivilstander = emptyList(),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
    )

val mockBarnAutomatiskBehandling2Fnr = "21131777002"
val mockBarnAutomatiskBehandling2 =
    PersonInfo(
        fødselsdato = LocalDate.now(),
        navn = "ARTIG MIDTPUNKT 2",
        kjønn = Kjønn.KVINNE,
        forelderBarnRelasjon = emptySet(),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = konstantAdresse,
        sivilstander = emptyList(),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
    )

val mockBarnAutomatiskBehandlingSkalFeileFnr = "21131777003"
val mockBarnAutomatiskBehandlingSkalFeile =
    PersonInfo(
        fødselsdato = LocalDate.now().minusMonths(2),
        navn = "ARTIG MIDTPUNKT 3",
        kjønn = Kjønn.KVINNE,
        forelderBarnRelasjon = emptySet(),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = alternaltivAdresse,
        sivilstander = emptyList(),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
    )

val mockSøkerAutomatiskBehandlingFnr = "04136226623"
val mockSøkerAutomatiskBehandlingAktør = lagAktør(mockSøkerAutomatiskBehandlingFnr)

val mockSøkerAutomatiskBehandling =
    PersonInfo(
        fødselsdato = LocalDate.parse("1962-08-04"),
        navn = "LEALAUS GYNGEHEST",
        kjønn = Kjønn.KVINNE,
        forelderBarnRelasjon =
            setOf(
                ForelderBarnRelasjon(
                    aktør = lagAktør(mockBarnAutomatiskBehandlingFnr),
                    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                    navn = null,
                    fødselsdato = null,
                    adressebeskyttelseGradering =
                    null,
                ),
            ),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = konstantAdresse,
        sivilstander = listOf(Sivilstand(type = SIVILSTANDTYPE.UGIFT, gyldigFraOgMed = null)),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
    )

fun genererAutomatiskTestperson(
    fødselsdato: LocalDate = LocalDate.parse("1998-10-10"),
    forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet(),
    sivilstander: List<Sivilstand> = emptyList(),
    bostedsadresser: List<Bostedsadresse> = konstantAdresse,
): PersonInfo =
    PersonInfo(
        fødselsdato = fødselsdato,
        navn = "Autogenerert Navn $fødselsdato",
        forelderBarnRelasjon =
            forelderBarnRelasjon
                .map {
                    ForelderBarnRelasjon(
                        aktør =
                            lagAktør(
                                it.aktør.personidenter
                                    .first()
                                    .fødselsnummer,
                            ),
                        relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                        navn = null,
                        fødselsdato = null,
                        adressebeskyttelseGradering =
                        null,
                    )
                }.toSet(),
        sivilstander = sivilstander,
        bostedsadresser = bostedsadresser,
    )

val mockNåværendeBosted =
    GrMatrikkeladresseBostedsadresse(
        matrikkelId = 123L,
        bruksenhetsnummer = "H301",
        tilleggsnavn = "navn",
        postnummer = "0202",
        poststed = "Oslo",
        kommunenummer = "2231",
    ).apply {
        periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1))
    }

val mockAnnetNåværendeBosted =
    GrMatrikkeladresseBostedsadresse(
        matrikkelId = 123L,
        bruksenhetsnummer = "H501",
        tilleggsnavn = "navn",
        postnummer = "0202",
        poststed = "Oslo",
        kommunenummer = "2231",
    ).apply {
        periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1))
    }

val mockTidligereBosted =
    GrMatrikkeladresseBostedsadresse(
        matrikkelId = 123L,
        bruksenhetsnummer = "H301",
        tilleggsnavn = "navn",
        postnummer = "0202",
        poststed = "Oslo",
        kommunenummer = "2231",
    ).apply {
        periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = LocalDate.now().minusYears(1))
    }
