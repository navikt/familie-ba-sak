package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør

fun mockPersonopplysningerService(dataFraCucumber: BegrunnelseTeksterStepDefinition): PersonopplysningerService {
    val personopplysningerService = mockk<PersonopplysningerService>()
    every { personopplysningerService.hentPersoninfoEnkel(any()) } answers {
        val aktør = firstArg<Aktør>()
        dataFraCucumber.persongrunnlag.values.flatMap { it.personer }.first { it.aktør == aktør }.tilPersonInfo()
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
