package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom.minusMonths(2),
                    tom = tom.minusMonths(2),
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
                    fom = fom,
                    tom = tom,
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
                    fom = fom,
                    tom = tom.plusMonths(2),
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

    @Test
    fun `Skal legge til alle barn med utbetaling ved utvidet barnetrygd`() {
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = arrayOf(søker, barn))
        val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UTVIDET_BARNETRYGD))
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(type = Vedtaksperiodetype.UTBETALING)
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerFnr = søker.personIdent.ident,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )
        val identerMedUtbetaling = listOf(barn.personIdent.ident)

        val personidenterForBegrunnelse = hentPersoneidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            persongrunnlag = persongrunnlag,
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            vilkårsvurdering = vilkårsvurdering,
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            identerMedUtbetaling = identerMedUtbetaling,
            endredeUtbetalingAndeler = emptyList(),
        )

        Assertions.assertEquals(listOf(barn.personIdent.ident, søker.personIdent.ident), personidenterForBegrunnelse)
    }

    @Test
    fun `Skal legge til alle barn fra endret utbetaling ved utvidet barnetrygd og endret utbetaling`() {
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val fom = LocalDate.now().withDayOfMonth(1)
        val tom = LocalDate.now().let {
            it.withDayOfMonth(it.lengthOfMonth())
        }

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = arrayOf(søker, barn2))
        val triggesAv = TriggesAv(vilkår = setOf(Vilkår.UTVIDET_BARNETRYGD))
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = fom,
            tom = tom,
        )
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerFnr = søker.personIdent.ident,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )

        val identerMedUtbetaling = listOf(barn1.personIdent.ident)
        val endredeUtbetalingAndeler = listOf(
            lagEndretUtbetalingAndel(
                person = barn2,
                fom = fom.toYearMonth(),
                tom = tom.toYearMonth(),
            )
        )

        val personidenterForBegrunnelse = hentPersoneidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            persongrunnlag = persongrunnlag,
            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            vilkårsvurdering = vilkårsvurdering,
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            identerMedUtbetaling = identerMedUtbetaling,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
        )

        Assertions.assertEquals(
            setOf(barn1.personIdent.ident, barn2.personIdent.ident, søker.personIdent.ident),
            personidenterForBegrunnelse.toSet()
        )
    }
}
