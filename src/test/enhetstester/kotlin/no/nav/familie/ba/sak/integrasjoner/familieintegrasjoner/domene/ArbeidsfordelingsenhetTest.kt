package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ArbeidsfordelingsenhetTest {
    @Nested
    inner class OpprettFra {
        @ParameterizedTest
        @EnumSource(BarnetrygdEnhet::class)
        fun `skal opprette arbeidsfordelingsenhet fra barnetrygdenhet`(enhet: BarnetrygdEnhet) {
            // Act
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(enhet)

            // Assert
            assertThat(arbeidsfordelingsenhet.enhetId).isEqualTo(enhet.enhetsnummer)
            assertThat(arbeidsfordelingsenhet.enhetNavn).isEqualTo(enhet.enhetsnavn)
        }
    }
}
