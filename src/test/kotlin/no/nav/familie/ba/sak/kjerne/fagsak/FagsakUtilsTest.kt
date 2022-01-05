package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

class FagsakUtilsTest {

    @Test
    fun `Skal finne begrunnelse på tidligere vedtaksperiode for samme fra- og med dato`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val fagsakErBegrunnet = FagsakUtils.fagsakBegrunnetMedBegrunnelse(
            vedtaksperiodeMedBegrunnelser = listOf(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom.førsteDagIInneværendeMåned(),
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.UTBETALING
                ).apply {
                    begrunnelser.addAll(
                        listOf(
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                            VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                        ).map { begrunnelse ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                vedtakBegrunnelseSpesifikasjon = begrunnelse
                            )
                        }
                    )
                }
            ),
            standardbegrunnelser = listOf(
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR,
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK
            ),
            måned = fom
        )

        assertTrue(fagsakErBegrunnet)
    }

    @Test
    fun `Skal ikke finne begrunnelse på tidligere vedtaksperiode når den er begrunnet for 1 år siden`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val fagsakErBegrunnet = FagsakUtils.fagsakBegrunnetMedBegrunnelse(
            vedtaksperiodeMedBegrunnelser = listOf(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom.minusYears(1).førsteDagIInneværendeMåned(),
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.UTBETALING
                ).apply {
                    begrunnelser.addAll(
                        listOf(
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                            VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK
                        ).map { begrunnelse ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                vedtakBegrunnelseSpesifikasjon = begrunnelse
                            )
                        }
                    )
                }
            ),
            standardbegrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK),
            måned = fom
        )

        assertFalse(fagsakErBegrunnet)
    }

    @Test
    fun `Skal ikke finne begrunnelse på tidligere vedtaksperiode når den ikke har begrunnelsen i det hele tatt`() {
        val fom = YearMonth.now().minusMonths(3)
        val vedtak = lagVedtak()

        val fagsakErBegrunnet = FagsakUtils.fagsakBegrunnetMedBegrunnelse(
            vedtaksperiodeMedBegrunnelser = listOf(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom.minusYears(1).førsteDagIInneværendeMåned(),
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.UTBETALING
                ).apply {
                    begrunnelser.addAll(
                        listOf(
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN,
                        ).map { begrunnelse ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                vedtakBegrunnelseSpesifikasjon = begrunnelse
                            )
                        }
                    )
                }
            ),
            standardbegrunnelser = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK),
            måned = fom
        )

        assertFalse(fagsakErBegrunnet)
    }
}
