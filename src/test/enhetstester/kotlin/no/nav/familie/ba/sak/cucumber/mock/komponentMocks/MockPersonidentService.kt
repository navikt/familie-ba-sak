package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService

fun mockPersonidentService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
): PersonidentService {
    val personidentService = mockk<PersonidentService>()
    every { personidentService.hentOgLagreAktør(any(), any()) } answers {
        val personIdent = firstArg<String>()
        dataFraCucumber.persongrunnlag
            .flatMap { it.value.personer }
            .first { it.aktør.aktivFødselsnummer() == personIdent }
            .aktør
    }
    every { personidentService.hentAktør(any()) } answers {
        val personIdent = firstArg<String>()
        dataFraCucumber.persongrunnlag
            .flatMap { it.value.personer }
            .first { it.aktør.aktivFødselsnummer() == personIdent }
            .aktør
    }
    every { personidentService.hentOgLagreAktørIder(any(), any()) } answers {
        val personIdenter = firstArg<List<String>>()
        dataFraCucumber.persongrunnlag
            .flatMap { it.value.personer }
            .distinctBy { it.aktør.aktivFødselsnummer() }
            .filter { it.aktør.aktivFødselsnummer() in personIdenter }
            .map { it.aktør }
    }
    return personidentService
}
