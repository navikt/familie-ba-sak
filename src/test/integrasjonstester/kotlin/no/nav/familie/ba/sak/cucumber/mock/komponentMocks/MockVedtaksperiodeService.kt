package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService

fun mockVedtaksperiodeService(): VedtaksperiodeService {
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(any()) } just runs
    return vedtaksperiodeService
}
