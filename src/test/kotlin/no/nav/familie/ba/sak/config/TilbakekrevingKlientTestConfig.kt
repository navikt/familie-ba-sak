package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
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

        every { tilbakekrevingKlient.hentForhåndsvisningVarselbrev(any()) } returns TEST_PDF

        every { tilbakekrevingKlient.opprettTilbakekrevingBehandling(any()) } returns "id1"

        every { tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(any()) } returns false

        every { tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(any()) } returns emptyList()

        return tilbakekrevingKlient
    }
}
