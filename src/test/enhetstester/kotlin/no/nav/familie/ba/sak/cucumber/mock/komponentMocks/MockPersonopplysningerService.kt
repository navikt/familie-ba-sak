package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør

fun mockPersonopplysningerService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): PersonopplysningerService {
    val personopplysningerService = mockk<PersonopplysningerService>()
    every { personopplysningerService.hentPersoninfoEnkel(any()) } answers {
        val aktør = firstArg<Aktør>()
        dataFraCucumber.persongrunnlag.values
            .flatMap { it.personer }
            .first { it.aktør == aktør }
            .tilPersonInfo()
    }

    every { personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(any()) } answers {
        val fødselsnummer = firstArg<Aktør>().personidenter.first().fødselsnummer
        lagPersonInfo(
            bostedsadresser = dataFraCucumber.adresser[fødselsnummer]?.bostedsadresser ?: emptyList(),
            oppholdsadresser = dataFraCucumber.adresser[fødselsnummer]?.oppholdsadresser ?: emptyList(),
            deltBosted = dataFraCucumber.adresser[fødselsnummer]?.deltBosted ?: emptyList(),
        )
    }
    return personopplysningerService
}

private fun Person.tilPersonInfo(): PersonInfo =
    PersonInfo(
        fødselsdato = fødselsdato,
        navn = navn,
        kjønn = kjønn,
        forelderBarnRelasjon = emptySet(),
        forelderBarnRelasjonMaskert = emptySet(),
        adressebeskyttelseGradering = null,
        bostedsadresser = emptyList(),
        sivilstander = emptyList(),
        opphold = emptyList(),
        statsborgerskap = emptyList(),
        dødsfall = null,
        kontaktinformasjonForDoedsbo = null,
    )
