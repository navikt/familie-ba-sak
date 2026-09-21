package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakBegrunnelseUtilTest {
    private val vedtaksperioder =
        listOf(
            lagVedtaksperiodeMedBegrunnelser(fom = LocalDate.of(2025, 10, 1), tom = LocalDate.of(2025, 10, 31), type = Vedtaksperiodetype.UTBETALING, begrunnelser = mutableSetOf()),
            lagVedtaksperiodeMedBegrunnelser(fom = LocalDate.of(2025, 11, 1), tom = LocalDate.of(2025, 11, 30), type = Vedtaksperiodetype.OPPHØR, begrunnelser = mutableSetOf()),
            lagVedtaksperiodeMedBegrunnelser(fom = LocalDate.of(2025, 12, 1), tom = LocalDate.of(2025, 12, 31), type = Vedtaksperiodetype.UTBETALING, begrunnelser = mutableSetOf()),
        )

    @Test
    fun `skal legge til begrunnelse i vedtaksperioden som starter i måneden`() {
        // Act
        leggTilBegrunnelseIVedtaksperiode(YearMonth.of(2025, 12), Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG, vedtaksperioder)

        // Assert
        assertThat(vedtaksperioder[2].begrunnelser.map { it.standardbegrunnelse }).containsExactly(Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG)
    }

    @Test
    fun `skal legge til begrunnelse i opphørsperiode når begrunnelsestypen er tillatt der`() {
        // Act
        leggTilBegrunnelseIVedtaksperiode(YearMonth.of(2025, 11), Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID, vedtaksperioder)

        // Assert
        assertThat(vedtaksperioder[1].begrunnelser.map { it.standardbegrunnelse }).containsExactly(Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID)
    }

    @Test
    fun `skal kaste feil når ingen vedtaksperiode med tillatt type starter i måneden`() {
        // Act & Assert
        val feil =
            assertThrows<Feil> {
                leggTilBegrunnelseIVedtaksperiode(YearMonth.of(2025, 11), Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG, vedtaksperioder)
            }
        assertThat(feil.message).isEqualTo("Finner ikke aktuell periode å begrunne ved autovedtak. Se securelogger for detaljer.")
    }
}
