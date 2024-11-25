package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrdinæreHjemlerKtTest {
    @Test
    fun `skal utlede en tom liste om ingen hjemler skal inkluderes`() {
        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                finnesVedtaksperiodeMedFritekst = false,
            )

        // Assert
        assertThat(ordinæreHjemler).isEmpty()
    }

    @Test
    fun `skal utlede ordinære hjemler for sanity begrunnelser`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(hjemler = listOf("1", "3", "2"))

        val sanityBegrunnelser = listOf(sanityBegrunnelse)

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEøsBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                finnesVedtaksperiodeMedFritekst = false,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "3", "2")
    }

    @Test
    fun `skal utlede ordinære hjemler for sanity eøs begrunnelser`() {
        // Arrange
        val sanityEøsBegrunnelse = lagSanityEøsBegrunnelse(hjemler = listOf("1", "3", "2"))

        val sanityEøsBegrunnelser = listOf(sanityEøsBegrunnelse)

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                finnesVedtaksperiodeMedFritekst = false,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "3", "2")
    }

    @Test
    fun `skal utlede opplysningsplikt hjemmel`() {
        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = true,
                finnesVedtaksperiodeMedFritekst = false,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("17", "18")
    }

    @Test
    fun `skal utlede fritekst hjemmeler`() {
        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = emptyList(),
                sanityEøsBegrunnelser = emptyList(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                finnesVedtaksperiodeMedFritekst = true,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("2", "4", "11")
    }

    @Test
    fun `skal utlede alle ordinære hjemler`() {
        // Arrange
        val sanityBegrunnelse = lagSanityBegrunnelse(hjemler = listOf("1", "3", "2"))

        val sanityBegrunnelser = listOf(sanityBegrunnelse)

        val sanityEøsBegrunnelse = lagSanityEøsBegrunnelse(hjemler = listOf("1", "6", "2"))

        val sanityEøsBegrunnelser = listOf(sanityEøsBegrunnelse)

        // Act
        val ordinæreHjemler =
            utledOrdinæreHjemler(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEøsBegrunnelser = sanityEøsBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = true,
                finnesVedtaksperiodeMedFritekst = true,
            )

        // Assert
        assertThat(ordinæreHjemler).containsOnly("1", "2", "3", "4", "6", "11", "17", "18")
    }
}
