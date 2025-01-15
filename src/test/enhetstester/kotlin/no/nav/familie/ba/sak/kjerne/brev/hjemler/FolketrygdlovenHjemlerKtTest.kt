package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.datagenerator.lagSanityBegrunnelse
import no.nav.familie.ba.sak.datagenerator.lagSanityEøsBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FolketrygdlovenHjemlerKtTest {
    @Test
    fun `skal returnere en tom liste om både eøs og standard sanity begrunnelser er tom`() {
        // Act
        val folketrygdlovenHjemler =
            utledFolketrygdlovenHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(folketrygdlovenHjemler).isEmpty()
    }

    @Test
    fun `skal returnere en liste med hjemler for folketrygdelove uten EØS om kun sanity begrunnelser ikke er tom`() {
        // Arrange
        val sanityBegrunnelse1 =
            lagSanityBegrunnelse(
                hjemlerFolketrygdloven = listOf("1", "4"),
            )

        val sanityBegrunnelse2 =
            lagSanityBegrunnelse(
                hjemlerFolketrygdloven = listOf("4", "3"),
            )

        val sanityBegrunnelser =
            listOf(
                sanityBegrunnelse1,
                sanityBegrunnelse2,
            )

        // Act
        val folketrygdlovenHjemler =
            utledFolketrygdlovenHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEøsBegrunnelser = emptyList(),
            )

        // Assert
        assertThat(folketrygdlovenHjemler).containsOnly("1", "4", "3")
    }

    @Test
    fun `skal returnere en liste med hjemler for folketrygdelove kun for EØS om kun sanity EØS begrunnelser ikke er tom`() {
        // Arrange
        val sanityEøsBegrunnelse1 =
            lagSanityEøsBegrunnelse(
                hjemlerFolketrygdloven = listOf("1", "6"),
            )

        val sanityEøsBegrunnelse2 =
            lagSanityEøsBegrunnelse(
                hjemlerFolketrygdloven = listOf("6", "3"),
            )

        val sanityEøsBegrunnelser =
            listOf(
                sanityEøsBegrunnelse1,
                sanityEøsBegrunnelse2,
            )

        // Act
        val folketrygdlovenHjemler =
            utledFolketrygdlovenHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = sanityEøsBegrunnelser,
            )

        // Assert
        assertThat(folketrygdlovenHjemler).containsOnly("1", "6", "3")
    }

    @Test
    fun `skal returnere en liste med hjemler for folketrygdelove inkludert de for EØS`() {
        // Arrange
        val sanityBegrunnelse1 =
            lagSanityBegrunnelse(
                hjemlerFolketrygdloven = listOf("1", "4"),
            )

        val sanityBegrunnelse2 =
            lagSanityBegrunnelse(
                hjemlerFolketrygdloven = listOf("4", "3"),
            )

        val sanityBegrunnelser =
            listOf(
                sanityBegrunnelse1,
                sanityBegrunnelse2,
            )

        val sanityEøsBegrunnelse1 =
            lagSanityEøsBegrunnelse(
                hjemlerFolketrygdloven = listOf("1", "6"),
            )

        val sanityEøsBegrunnelse2 =
            lagSanityEøsBegrunnelse(
                hjemlerFolketrygdloven = listOf("6", "3"),
            )

        val sanityEøsBegrunnelser =
            listOf(
                sanityEøsBegrunnelse1,
                sanityEøsBegrunnelse2,
            )

        // Act
        val folketrygdlovenHjemler =
            utledFolketrygdlovenHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEøsBegrunnelser = sanityEøsBegrunnelser,
            )

        // Assert
        assertThat(folketrygdlovenHjemler).containsOnly("1", "4", "6", "3")
    }
}
