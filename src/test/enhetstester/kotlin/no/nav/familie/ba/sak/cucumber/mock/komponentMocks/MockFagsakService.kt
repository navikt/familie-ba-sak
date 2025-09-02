package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør

fun mockFagsakService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): FagsakService {
    val fagsakService = mockk<FagsakService>()
    every { fagsakService.hentNormalFagsak(any()) } answers {
        val aktør = firstArg<Aktør>()
        dataFraCucumber.fagsaker.values.single { it.aktør == aktør }
    }
    every { fagsakService.hentFagsakerPåPerson(any()) } answers {
        listOf(dataFraCucumber.fagsaker.values.first())
    }
    every { fagsakService.oppdaterStatus(any(), any()) } answers {
        val fagsak = firstArg<Fagsak>()
        val nyStatus = secondArg<FagsakStatus>()

        fagsak.status = nyStatus
        fagsak
    }
    every { fagsakService.hentAktør(any()) } answers {
        val fagsakId = firstArg<Long>()
        dataFraCucumber.fagsaker.values
            .single { it.id == fagsakId }
            .aktør
    }
    return fagsakService
}
