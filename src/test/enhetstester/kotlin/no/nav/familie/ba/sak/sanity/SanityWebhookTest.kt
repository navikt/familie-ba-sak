package no.nav.familie.ba.sak.sanity

import no.nav.familie.ba.sak.testfiler.Testfil.SANITY_BEGRUNNELSER
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SanityWebhookTest {
    @Test
    fun `skal feile når visningsnavn endres i Sanity`() {
        // Arrange
        val begrunnelser = jsonMapper.readTree(SANITY_BEGRUNNELSER.readText())

        // Act
        val begrunnelse = begrunnelser.single { it.get("apiNavn").asString() == "innvilgetTilleggstekstOpphorUtvidetNyfoedtBarn" }

        // Assert
        assertThat(begrunnelse.get("visningsnavn").asString()).isEqualTo("[Utgått] Tilleggstekst opphør utvidet nyfødt barn")
    }
}
