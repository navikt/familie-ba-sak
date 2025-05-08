package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ba.sak.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnhetConfigTest {

    private val enhetConfig = EnhetConfig().apply {
        enheter =
            mapOf(
                Pair("OSLO", "gruppeIdForOslo"),
                Pair("DRAMMEN", "gruppeIdForDrammen"),
                Pair("STEINKJER", "gruppeIdForSteinkjer")
            )
    }

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    fun `Skal ikke returnere noen enheter dersom bruker ikke har grupper som tilsvarer noen av enhetene`() {
        // Act
        val enheterBrukerHarTilgangTil = enhetConfig.hentAlleEnheterBrukerHarTilgangTil()

        // Assert
        assertThat(enheterBrukerHarTilgangTil).isEmpty()
    }

    @Test
    fun `Skal  returnere alle enheter bruker har tilgang til dersom det er flere`() {
        // Arrange
        mockBrukerContext(groups = listOf("gruppeIdForDrammen", "gruppeIdForSteinkjer"))

        // Act
        val enheterBrukerHarTilgangTil = enhetConfig.hentAlleEnheterBrukerHarTilgangTil()

        // Assert
        assertThat(enheterBrukerHarTilgangTil).containsExactly(BarnetrygdEnhet.DRAMMEN, BarnetrygdEnhet.STEINKJER)
    }
}

