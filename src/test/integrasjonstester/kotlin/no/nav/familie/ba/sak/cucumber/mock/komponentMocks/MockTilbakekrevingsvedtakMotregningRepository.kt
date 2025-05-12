package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningRepository

fun mockTilbakekrevingsvedtakMotregningRepository(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): TilbakekrevingsvedtakMotregningRepository {
    val tilbakekrevingsvedtakMotregningRepository = mockk<TilbakekrevingsvedtakMotregningRepository>()

    every { tilbakekrevingsvedtakMotregningRepository.finnTilbakekrevingsvedtakMotregningForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        dataFraCucumber.tilbakekrevingsvedtakMotregning[behandlingId]
    }

    return tilbakekrevingsvedtakMotregningRepository
}
