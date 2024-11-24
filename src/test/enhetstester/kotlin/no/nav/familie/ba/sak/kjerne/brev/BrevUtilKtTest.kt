package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrevUtilKtTest {
    @Test
    fun `skal gi riktig dato for opphørstester`() {
        // Arrange
        val sisteFom = LocalDate.now().minusMonths(2)

        val opphørsperioder =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().minusYears(1).plusMonths(2),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusMonths(5),
                    tom = LocalDate.now().minusMonths(4),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = sisteFom,
                    tom = LocalDate.now(),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
            )

        // Act
        val virkningstidspunktForDødsfallbrev = hentVirkningstidspunktForDødsfallbrev(opphørsperioder, 0L)

        // Assert
        assertThat(virkningstidspunktForDødsfallbrev).isEqualTo(sisteFom.tilMånedÅr())
    }
}
