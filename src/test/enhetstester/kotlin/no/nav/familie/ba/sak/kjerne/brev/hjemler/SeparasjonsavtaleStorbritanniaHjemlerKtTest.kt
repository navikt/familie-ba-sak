package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.datagenerator.lagSanityEøsBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SeparasjonsavtaleStorbritanniaHjemlerKtTest {
    @Test
    fun `skal returnere en tom liste når sanity eøs begrunnelser er tom`() {
        // Act
        val seprasjonsavtaleStorbritanniaHjemler =
            utledSeprasjonsavtaleStorbritanniaHjemler(
                sanityEøsBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(seprasjonsavtaleStorbritanniaHjemler).isEmpty()
    }

    @Test
    fun `skal utlede seprasjonsavtale for storbritannia hjemler`() {
        // Arrange
        val sanityEøsBegrunnelse =
            lagSanityEøsBegrunnelse(
                hjemlerSeperasjonsavtalenStorbritannina = listOf("1", "4", "3", "4"),
            )

        val sanityEØSBegrunnelser = listOf(sanityEøsBegrunnelse)

        // Act
        val seprasjonsavtaleStorbritanniaHjemler =
            utledSeprasjonsavtaleStorbritanniaHjemler(
                sanityEøsBegrunnelser = sanityEØSBegrunnelser,
            )

        // Assert
        assertThat(seprasjonsavtaleStorbritanniaHjemler).containsOnly("1", "4", "3")
    }
}
