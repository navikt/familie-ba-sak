package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class VedtaksperiodeServiceUtilsTest {
    @Test
    fun `skal slå sammen endrede utbetalingsandeler med samme prosent som overlapper`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = person1,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person1,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                ),
                lagAndelTilkjentYtelse(
                    person = person2,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person2,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
                vedtak = vedtak,
                andelerTilkjentYtelse = andelTilkjentYtelser
            )

        Assertions.assertEquals(1, endredeUtbetalingsperioderMedBegrunnelser.size)

        Assertions.assertEquals(
            setOf(person2.personIdent.ident, person1.personIdent.ident),
            endredeUtbetalingsperioderMedBegrunnelser.single().begrunnelser.single().personIdenter.toSet()
        )
    }

    @Test
    fun `Skal ikke slå sammen endrede utbetalingsperioder som overlapper, men har ulik prosent`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = person1,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(100),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person1,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                ),
                lagAndelTilkjentYtelse(
                    person = person2,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person2,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
                vedtak = vedtak,
                andelerTilkjentYtelse = andelTilkjentYtelser
            )

        Assertions.assertEquals(2, endredeUtbetalingsperioderMedBegrunnelser.size)
    }

    @Test
    fun `skal ikke legge til personer på begrunnelse dersom begrunnelsen ikke tilhører dem`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = person1,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person1,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                ),
                lagAndelTilkjentYtelse(
                    person = person2,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person2,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER)
                        )
                    )
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser = hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
            vedtak = vedtak,
            andelerTilkjentYtelse = andelTilkjentYtelser
        )
        val begrunnelser = endredeUtbetalingsperioderMedBegrunnelser
            .single()
            .begrunnelser

        Assertions.assertEquals(2, begrunnelser.size)

        val begrunnelsePerson1 =
            begrunnelser
                .find {
                    it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
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

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = person1,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person1,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                ),
                lagAndelTilkjentYtelse(
                    person = person2,
                    fom = fom.minusMonths(2).toString(),
                    tom = tom.minusMonths(2).toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person2,
                            fom = fom.minusMonths(2),
                            tom = tom.minusMonths(2),
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER)
                        )
                    )
                )
            )

        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser = hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
            vedtak = vedtak,
            andelerTilkjentYtelse = andelTilkjentYtelser
        )

        Assertions.assertEquals(2, endredeUtbetalingsperioderMedBegrunnelser.size)
    }

    @Test
    fun `Skal slå sammen endrede utbetalingsperioder med samme prosent som delvis overlapper`() {
        val person1 = lagPerson()
        val person2 = lagPerson()
        val fom = YearMonth.now().minusMonths(1)
        val tom = YearMonth.now()

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = person1,
                    fom = fom.toString(),
                    tom = tom.toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person1,
                            fom = fom,
                            tom = tom,
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                ),
                lagAndelTilkjentYtelse(
                    person = person2,
                    fom = fom.toString(),
                    tom = tom.plusMonths(2).toString(),
                    prosent = BigDecimal(0),
                    endretUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            person = person2,
                            fom = fom,
                            tom = tom.plusMonths(2),
                            vedtakBegrunnelseSpesifikasjoner = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM)
                        )
                    )
                )
            )
        val vedtak = lagVedtak()
        val endredeUtbetalingsperioderMedBegrunnelser =
            hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
                vedtak = vedtak,
                andelerTilkjentYtelse = andelTilkjentYtelser
            ).sortedBy { it.fom }

        Assertions.assertEquals(2, endredeUtbetalingsperioderMedBegrunnelser.size)

        Assertions.assertEquals(
            fom,
            endredeUtbetalingsperioderMedBegrunnelser.first().fom!!.toYearMonth()
        )

        Assertions.assertEquals(
            tom,
            endredeUtbetalingsperioderMedBegrunnelser.first().tom!!.toYearMonth()
        )

        Assertions.assertEquals(
            tom.plusMonths(1),
            endredeUtbetalingsperioderMedBegrunnelser.last().fom!!.toYearMonth()
        )

        Assertions.assertEquals(
            tom.plusMonths(2),
            endredeUtbetalingsperioderMedBegrunnelser.last().tom!!.toYearMonth()
        )
    }
}
