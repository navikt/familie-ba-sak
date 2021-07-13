package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import java.time.LocalDate

val konstantAdresse: List<Bostedsadresse> =
        listOf(Bostedsadresse(
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                vegadresse = Vegadresse(matrikkelId = 6367230663,
                                        husnummer = "36",
                                        husbokstav = "D",
                                        bruksenhetsnummer = null,
                                        adressenavn = "Arnulv Eide -veien",
                                        kommunenummer = "5422",
                                        tilleggsnavn = null,
                                        postnummer = "9050"),
        ))


fun genererAutomatiskTestperson(
    fødselsdato: LocalDate = LocalDate.parse("1998-10-10"),
    forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet(),
    sivilstander: List<Sivilstand> = emptyList(),
    bostedsadresse: List<Bostedsadresse> = konstantAdresse
): PersonInfo {
    return PersonInfo(
        fødselsdato = fødselsdato,
        navn = "Autogenerert Navn $fødselsdato",
        forelderBarnRelasjon = forelderBarnRelasjon.map {
            ForelderBarnRelasjon(
                personIdent = Personident(id = it.toString()),
                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                navn = null,
                fødselsdato = null,
                        adressebeskyttelseGradering =
                        null,
                )
            }.toSet(),
            sivilstander = sivilstander,
            bostedsadresser = bostedsadresse)
}

val mockBarnAutomatiskBehandling = PersonInfo(fødselsdato = LocalDate.parse("2017-11-21"),
                                              navn = "ARTIG MIDTPUNKT",
                                              kjønn = Kjønn.KVINNE,
                                              forelderBarnRelasjon = emptySet(),
                                              forelderBarnRelasjonMaskert = emptySet(),
                                              adressebeskyttelseGradering = null,
                                              bostedsadresser = listOf(Bostedsadresse(
                                                      gyldigFraOgMed = null,
                                                      gyldigTilOgMed = null,
                                                      vegadresse = Vegadresse(matrikkelId = 6367230663,
                                                                              husnummer = "36",
                                                                              husbokstav = "D",
                                                                              bruksenhetsnummer = null,
                                                                              adressenavn = "Arnulv Eide -veien",
                                                                              kommunenummer = "5422",
                                                                              tilleggsnavn = null,
                                                                              postnummer = "9050"),
                                                      matrikkeladresse = null,
                                                      ukjentBosted = null,
                                              )),
                                              sivilstander = emptyList(),
                                              opphold = emptyList(),
                                              statsborgerskap = emptyList())


val mockSøkerAutomatiskBehandling = PersonInfo(fødselsdato = LocalDate.parse("1962-08-04"),
                                               navn = "LEALAUS GYNGEHEST",
                                               kjønn = Kjønn.KVINNE,
                                               forelderBarnRelasjon = setOf(ForelderBarnRelasjon(
                                                       personIdent = Personident(id = "21111777001"),
                                                       relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                                       navn = null,
                                                       fødselsdato = null,
                                                       adressebeskyttelseGradering =
                                                       null,
                                               )),
                                               forelderBarnRelasjonMaskert = emptySet(),
                                               adressebeskyttelseGradering = null,
                                               bostedsadresser = listOf(Bostedsadresse(
                                                       gyldigFraOgMed = null,
                                                       gyldigTilOgMed = null,
                                                       vegadresse = Vegadresse(matrikkelId = 6367230663,
                                                                               husnummer = "36",
                                                                               husbokstav = "D",
                                                                               bruksenhetsnummer = null,
                                                                               adressenavn = "Arnulv Eide -veien",
                                                                               kommunenummer = "5422",
                                                                               tilleggsnavn = null,
                                                                               postnummer = "9050"),
                                                       matrikkeladresse = null,
                                                       ukjentBosted = null,
                                               )),
                                               sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT, gyldigFraOgMed = null)),
                                               opphold = emptyList(),
                                               statsborgerskap = emptyList())


val mockNåværendeBosted = GrMatrikkeladresse(matrikkelId = 123L,
                                             bruksenhetsnummer = "H301",
                                             tilleggsnavn = "navn",
                                             postnummer = "0202",
                                             kommunenummer = "2231").apply {
    periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1))
}

val mockAnnetNåværendeBosted = GrMatrikkeladresse(matrikkelId = 123L,
                                                  bruksenhetsnummer = "H501",
                                                  tilleggsnavn = "navn",
                                                  postnummer = "0202",
                                                  kommunenummer = "2231").apply {
    periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1))
}

val mockTidligereBosted = GrMatrikkeladresse(matrikkelId = 123L,
                                             bruksenhetsnummer = "H301",
                                             tilleggsnavn = "navn",
                                             postnummer = "0202",
                                             kommunenummer = "2231").apply {
    periode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = LocalDate.now().minusYears(1))
}
val mockTidløstBosted = GrMatrikkeladresse(matrikkelId = 123L,
                                           bruksenhetsnummer = "H301",
                                           tilleggsnavn = "navn",
                                           postnummer = "0202",
                                           kommunenummer = "2231")

val personopplysningGrunnlagForGodkjentSak = lagTestPersonopplysningGrunnlag(1L,
                                                                             "04086226621",
                                                                             mutableListOf("21111777001")).apply {
    søker.bostedsadresser = mutableListOf(mockNåværendeBosted)
    barna.forEach { it.bostedsadresser = mutableListOf(mockNåværendeBosted) }
}

val personopplysningGrunnlagMedUtdatertAdresse = lagTestPersonopplysningGrunnlag(1L,
                                                                                 "04086226621",
                                                                                 mutableListOf("21111777001")).apply {
    søker.bostedsadresser = mutableListOf(mockTidligereBosted)
    barna.forEach { it.bostedsadresser = mutableListOf(mockNåværendeBosted) }
}

val personopplysningGrunnlagMedUlikeAdresser = lagTestPersonopplysningGrunnlag(1L,
                                                                               "04086226621",
                                                                               mutableListOf("21111777001")).apply {
    søker.bostedsadresser = mutableListOf(mockNåværendeBosted)
    barna.forEach { it.bostedsadresser = mutableListOf(mockAnnetNåværendeBosted) }
}

val personopplysningGrunnlagMedUlikeAdresserForEtAvFlereBarn =
        lagTestPersonopplysningGrunnlag(1L,
                                        "04086226621",
                                        mutableListOf("21111777001",
                                                      "21111777002")).apply {
            søker.bostedsadresser = mutableListOf(mockNåværendeBosted)
            barna.first().bostedsadresser = mutableListOf(mockAnnetNåværendeBosted)
            barna.last().bostedsadresser = mutableListOf(mockNåværendeBosted)
        }