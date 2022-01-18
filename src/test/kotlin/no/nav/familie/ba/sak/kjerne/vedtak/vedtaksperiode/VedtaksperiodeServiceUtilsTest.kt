package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.periodeErOppyltForYtelseTypeGammel
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
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
                            vedtakBegrunnelseSpesifikasjoner = listOf(
                                VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
                            )
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
                            vedtakBegrunnelseSpesifikasjoner = listOf(
                                VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
                            )
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
                            vedtakBegrunnelseSpesifikasjoner = listOf(
                                VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
                            )
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
                            vedtakBegrunnelseSpesifikasjoner = listOf(
                                VedtakBegrunnelseSpesifikasjon.INNVILGET_VURDERING_HELE_FAMILIEN_PLIKTIG_MEDLEM
                            )
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
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )
        val identerMedUtbetaling = listOf(barn.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer())

        val personidenterForBegrunnelse = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            periode = NullablePeriode(LocalDate.now().minusMonths(1), null),
            brevGrunnlag = BrevGrunnlag(
                minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                personerPåBehandling = persongrunnlag.personer.map { it.tilMinimertPerson() },
                minimerteEndredeUtbetalingAndeler = emptyList(),
            ),
            identerMedUtbetalingPåPeriode = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = false
        )

        Assertions.assertEquals(
            listOf(barn.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer()),
            personidenterForBegrunnelse
        )
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
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )

        val identerMedUtbetaling = listOf(barn1.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer())
        val endredeUtbetalingAndeler = listOf(
            lagEndretUtbetalingAndel(
                person = barn2,
                fom = fom.toYearMonth(),
                tom = tom.toYearMonth(),
            )
        )

        val personidenterForBegrunnelse = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            periode = NullablePeriode(fom, tom),
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            brevGrunnlag = BrevGrunnlag(
                minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                personerPåBehandling = persongrunnlag.personer.map { it.tilMinimertPerson() },
                minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler
                    .map { it.tilMinimertRestEndretUtbetalingAndel() },
            ),
            identerMedUtbetalingPåPeriode = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = false
        )

        Assertions.assertEquals(
            setOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer()),
            personidenterForBegrunnelse.toSet()
        )
    }

    val ytelseTyperSmåbarnstillegg =
        listOf(YtelseType.SMÅBARNSTILLEGG, YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperUtvidetOgOrdinær =
        listOf(YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperOrdinær =
        listOf(YtelseType.ORDINÆR_BARNETRYGD)

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger ved innvilget VedtakBegrunnelseType`() {

        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                andelerTilkjentYtelse = emptyList(),
                fomForPeriode = LocalDate.now()
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = emptyList(),
                fomForPeriode = LocalDate.now()
            )
        )
    }

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger når VedtakBegrunnelseType er reduksjon`() {
        val fom = LocalDate.now().førsteDagIInneværendeMåned()

        val andelerTilkjentYtelseMedSmåbarnstilleggIkkeDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(5).toYearMonth(),
                tom = fom.minusMonths(5).toYearMonth(),
                ytelseType = YtelseType.SMÅBARNSTILLEGG
            )
        )

        val andelerTilkjentYtelseMedOrdinærYtelseDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(1).toYearMonth(),
                tom = fom.minusMonths(1).toYearMonth(),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            )
        )

        val andelerTilkjentYtelseMedSmåbarnstilleggDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(1).toYearMonth(),
                tom = fom.minusMonths(1).toYearMonth(),
                ytelseType = YtelseType.SMÅBARNSTILLEGG
            )
        )

        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedSmåbarnstilleggDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedSmåbarnstilleggDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedOrdinærYtelseDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedSmåbarnstilleggIkkeDagenFør,
                fomForPeriode = fom
            )
        )
    }

    @Test
    fun `Skal gi false når VedtakBegrunnelseType ikke er innvilget eller reduksjon `() {

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.AVSLAG.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                andelerTilkjentYtelse = emptyList(),
                fomForPeriode = LocalDate.now()
            )
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet-trigger ved innvilget`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = emptyList(),
                fomForPeriode = LocalDate.now()
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                andelerTilkjentYtelse = emptyList(),
                fomForPeriode = LocalDate.now()
            )
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet barnetrygd-trigger når VedtakBegrunnelseType er reduksjon`() {
        val fom = LocalDate.now().førsteDagIInneværendeMåned()

        val andelerTilkjentYtelseMedUtvidetBarnetrygdIkkeDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(5).toYearMonth(),
                tom = fom.minusMonths(5).toYearMonth(),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            )
        )

        val andelerTilkjentYtelseMedOrdinærYtelseDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(1).toYearMonth(),
                tom = fom.minusMonths(1).toYearMonth(),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            )
        )

        val andelerTilkjentYtelseMedUtvidetBarnetrygdDagenFør = listOf(
            lagAndelTilkjentYtelse(
                fom = fom.minusMonths(1).toYearMonth(),
                tom = fom.minusMonths(1).toYearMonth(),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD
            )
        )

        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedUtvidetBarnetrygdDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedUtvidetBarnetrygdDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedOrdinærYtelseDagenFør,
                fomForPeriode = fom
            )
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseTypeGammel(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                andelerTilkjentYtelse = andelerTilkjentYtelseMedUtvidetBarnetrygdIkkeDagenFør,
                fomForPeriode = fom
            )
        )
    }
}
