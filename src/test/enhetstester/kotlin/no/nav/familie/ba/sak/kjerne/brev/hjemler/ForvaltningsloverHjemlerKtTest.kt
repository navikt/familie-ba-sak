package no.nav.familie.ba.sak.kjerne.brev.hjemler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForvaltningsloverHjemlerKtTest {
    @Test
    fun `skal utlede forvaltningslover hjemler om vedtak korrigert hjemmel skal med i brev`() {
        // Act
        val forvaltningsloverHjemler = utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = true)

        // Assert
        assertThat(forvaltningsloverHjemler).containsOnly("35")
    }

    @Test
    fun `skal utlede forvaltningslover hjemler om vedtak korrigert hjemmel ikke skal med i brev`() {
        // Act
        val forvaltningsloverHjemler = utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = false)

        // Assert
        assertThat(forvaltningsloverHjemler).isEmpty()
    }
}
