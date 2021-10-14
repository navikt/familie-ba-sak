package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class VedtaksperiodeServiceUtilsTest {
    @Test
    fun `skal slå sammen endrede utbetalingsandeler med samme fom og tom`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()
        val endredeUtbetalingsandeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = person1,
                    fom = fom,
                    tom = tom,
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM),
                ),
                lagEndretUtbetalingAndel(
                    person = person2,
                    fom = fom,
                    tom = tom,
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentEndredeUtbetalingsperioderMedBegrunnelser(vedtak, endredeUtbetalingsandeler)

        Assertions.assertEquals(1, endredeUtbetalingsperioderMedBegrunnelser.size)

        Assertions.assertEquals(
            setOf(person2.personIdent.ident, person1.personIdent.ident),
            endredeUtbetalingsperioderMedBegrunnelser.single().begrunnelser.single().personIdenter.toSet()
        )
    }

    @Test
    fun `skal ikke legge til personer på begrunnelse dersom begrunnelsen ikke tilhører dem`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()
        val endredeUtbetalingsandeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = person1,
                    fom = fom,
                    tom = tom,
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                ),
                lagEndretUtbetalingAndel(
                    person = person2,
                    fom = fom,
                    tom = tom,
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER)
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentEndredeUtbetalingsperioderMedBegrunnelser(vedtak, endredeUtbetalingsandeler)
        val begrunnelser = endredeUtbetalingsperioderMedBegrunnelser
            .single()
            .begrunnelser

        Assertions.assertEquals(2, begrunnelser.size)

        val begrunnelsePerson1 =
            begrunnelser
                .find {
                    it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
                }

        Assertions.assertEquals(
            listOf(person1.personIdent.ident),
            begrunnelsePerson1?.personIdenter
        )
    }

    @Test
    fun `skal ikke slå sammen endrede utbetalingsandeler med ulik fom og tom`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()
        val endredeUtbetalingsandeler =
            listOf(
                lagEndretUtbetalingAndel(
                    person = person1,
                    fom = fom,
                    tom = tom,
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGELSE_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                ),
                lagEndretUtbetalingAndel(
                    person = person2,
                    fom = fom.minusMonths(2),
                    tom = tom.minusMonths(2),
                    vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER)
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentEndredeUtbetalingsperioderMedBegrunnelser(vedtak, endredeUtbetalingsandeler)

        Assertions.assertEquals(2, endredeUtbetalingsperioderMedBegrunnelser.size)
    }
}
