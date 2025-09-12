package no.nav.familie.ba.sak.fake

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.testfiler.Testfil
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class TilbakekrevingKlientTestConfig {
    @Bean
    @Profile("mock-tilbakekreving-klient")
    @Primary
    fun mockTilbakekrevingKlient(): TilbakekrevingKlient {
        val tilbakekrevingKlient: TilbakekrevingKlient = mockk()

        clearTilbakekrevingKlientMocks(tilbakekrevingKlient)

        return tilbakekrevingKlient
    }

    companion object {
        fun clearTilbakekrevingKlientMocks(mockTilbakekrevingKlient: TilbakekrevingKlient) {
            clearMocks(mockTilbakekrevingKlient)

            every { mockTilbakekrevingKlient.hentForhåndsvisningVarselbrev(any()) } returns Testfil.TEST_PDF

            every { mockTilbakekrevingKlient.opprettTilbakekrevingBehandling(any()) } returns "id1"

            every { mockTilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(any()) } returns false

            every { mockTilbakekrevingKlient.hentTilbakekrevingsbehandlinger(any()) } returns emptyList()
        }
    }
}
