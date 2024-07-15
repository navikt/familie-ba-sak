package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService

fun mockPersonidentService(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    nyBehandlingId: Long,
): PersonidentService {
    val personidentService = mockk<PersonidentService>()
    every { personidentService.hentOgLagreAktør(any(), any()) } answers {
        val personIdent = firstArg<String>()
        dataFraCucumber.persongrunnlag[nyBehandlingId]!!
            .personer
            .single { it.aktør.aktivFødselsnummer() == personIdent }
            .aktør
    }
    every { personidentService.hentAktør(any()) } answers {
        val personIdent = firstArg<String>()
        dataFraCucumber.persongrunnlag[nyBehandlingId]!!
            .personer
            .single { it.aktør.aktivFødselsnummer() == personIdent }
            .aktør
    }
    return personidentService
}
