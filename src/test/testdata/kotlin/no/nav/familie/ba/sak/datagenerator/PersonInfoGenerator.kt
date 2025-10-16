package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsbo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import java.time.LocalDate

fun lagPersonInfo(
    fødselsdato: LocalDate = LocalDate.now().minusYears(30),
    navn: String? = "Fornavn Etternavn",
    kjønn: Kjønn = Kjønn.UKJENT,
    forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet(),
    forelderBarnRelasjonMaskert: Set<ForelderBarnRelasjonMaskert> = emptySet(),
    adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    bostedsadresser: List<Bostedsadresse> = emptyList(),
    oppholdsadresser: List<Oppholdsadresse> = emptyList(),
    deltBosted: List<DeltBosted> = emptyList(),
    sivilstander: List<Sivilstand> = emptyList(),
    opphold: List<Opphold>? = emptyList(),
    statsborgerskap: List<Statsborgerskap>? = emptyList(),
    dødsfall: DødsfallData? = null,
    kontaktinformasjonForDoedsbo: PdlKontaktinformasjonForDødsbo? = null,
    erEgenAnsatt: Boolean? = null,
) = PersonInfo(
    fødselsdato = fødselsdato,
    navn = navn,
    kjønn = kjønn,
    forelderBarnRelasjon = forelderBarnRelasjon,
    forelderBarnRelasjonMaskert = forelderBarnRelasjonMaskert,
    adressebeskyttelseGradering = adressebeskyttelseGradering,
    bostedsadresser = bostedsadresser,
    oppholdsadresser = oppholdsadresser,
    deltBosted = deltBosted,
    sivilstander = sivilstander,
    opphold = opphold,
    statsborgerskap = statsborgerskap,
    dødsfall = dødsfall,
    kontaktinformasjonForDoedsbo = kontaktinformasjonForDoedsbo,
    erEgenAnsatt = erEgenAnsatt,
)
