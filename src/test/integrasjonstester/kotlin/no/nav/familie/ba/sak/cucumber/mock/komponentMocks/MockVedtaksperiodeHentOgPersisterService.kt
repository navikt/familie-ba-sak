package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService

fun mockVedtaksperiodeHentOgPersisterService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): VedtaksperiodeHentOgPersisterService {
    val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(any()) } answers {
        val vedtak = firstArg<Vedtak>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser.filter { it.vedtak.id != vedtak.id }
    }
    every { vedtaksperiodeHentOgPersisterService.lagre(any<List<VedtaksperiodeMedBegrunnelser>>()) } answers {
        val vedtaksperioder = firstArg<List<VedtaksperiodeMedBegrunnelser>>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser + vedtaksperioder
        vedtaksperioder
    }
    every { vedtaksperiodeHentOgPersisterService.lagre(any<VedtaksperiodeMedBegrunnelser>()) } answers {
        val vedtaksperiode = firstArg<VedtaksperiodeMedBegrunnelser>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser = dataFraCucumber.vedtaksperioderMedBegrunnelser + vedtaksperiode
        vedtaksperiode
    }
    every { vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(any()) } answers {
        val vedtakId = firstArg<Long>()
        dataFraCucumber.vedtaksperioderMedBegrunnelser.filter { it.vedtak.id == vedtakId }
    }
    return vedtaksperiodeHentOgPersisterService
}
