package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.Companion.erGyldigBehandlendeBarnetrygdEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BarnetrygdEnhetTest {
    @Nested
    inner class ErGyldigBehandlendeBarnetrygdEnhetTest {
        @Test
        fun `skal returnere false hvis ugyldig enhetsnummer`() {
            // Act
            val erGyldig = erGyldigBehandlendeBarnetrygdEnhet("1")

            // Assert
            assertThat(erGyldig).isFalse()
        }

        @Test
        fun `skal returnere false hvis enhetsnummer er 4863 midlertidig enhet`() {
            // Act
            val erGyldig = erGyldigBehandlendeBarnetrygdEnhet(BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer)

            // Assert
            assertThat(erGyldig).isFalse()
        }

        @Test
        fun `skal returnere true hvis gyldig enhetsnummer`() {
            // Act
            val erGyldig = erGyldigBehandlendeBarnetrygdEnhet(BarnetrygdEnhet.OSLO.enhetsnummer)

            // Assert
            assertThat(erGyldig).isTrue()
        }
    }
}
