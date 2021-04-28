package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingKlient
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

        return tilbakekrevingKlient
    }
}