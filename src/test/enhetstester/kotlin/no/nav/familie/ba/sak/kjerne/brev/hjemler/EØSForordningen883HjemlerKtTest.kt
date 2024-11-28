package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EØSForordningen883HjemlerKtTest {
    @Test
    fun `skal returnere en tom liste om input er en tom liste`() {
        // Act
        val eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityEøsBegrunnelser = emptyList())

        // Assert
        assertThat(eøsForordningen883Hjemler).isEmpty()
    }

    @Test
    fun `skal utlede EØS forordningen 883 hjemler`() {
        // Arrange
        val sanityEøsBegrunnelse1 =
            lagSanityEøsBegrunnelse(
                hjemlerEØSForordningen883 = listOf("6", "4", "3"),
            )

        val sanityEøsBegrunnelse2 =
            lagSanityEøsBegrunnelse(
                hjemlerEØSForordningen883 = listOf("2", "1", "6"),
            )

        val sanityEøsBegrunnelser = listOf(sanityEøsBegrunnelse1, sanityEøsBegrunnelse2)

        // Act
        val eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser)

        // Assert
        assertThat(eøsForordningen883Hjemler).containsOnly("6", "4", "3", "2", "1")
    }
}
