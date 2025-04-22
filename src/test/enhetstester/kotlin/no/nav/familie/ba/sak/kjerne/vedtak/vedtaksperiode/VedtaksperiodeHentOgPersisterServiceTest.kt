package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperiodeHentOgPersisterServiceTest {
    val vedtaksperiodeRepository = mockk<VedtaksperiodeRepository>()
    val vedtaksperiodeHentOgPersisterService = VedtaksperiodeHentOgPersisterService(vedtaksperiodeRepository)

    @Test
    fun `hentVedtaksperiodeThrows skal returnere vedtaksperiode med begrunnelser dersom det finnes`() {
        // Arrange
        val mocketVedtaksperiodeMedBegrunnelser = mockk<VedtaksperiodeMedBegrunnelser>()
        every { vedtaksperiodeRepository.hentVedtaksperiode(1) } returns mocketVedtaksperiodeMedBegrunnelser

        // Act
        val hentetVedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(1)

        // Assert
        assertThat(hentetVedtaksperiodeMedBegrunnelser).isSameAs(mocketVedtaksperiodeMedBegrunnelser)
    }

    @Test
    fun `hentVedtaksperiodeThrows skal kaste feil hvis det ikke finnes noe vedtaksperiode med id`() {
        // Arrange & Act
        every { vedtaksperiodeRepository.hentVedtaksperiode(1) } returns null

        val feil =
            assertThrows<Feil> {
                vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(1)
            }

        // Assert
        assertThat(feil.message).isEqualTo("Fant ingen vedtaksperiode med id 1")
    }
}
