package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.dataGenerator.brev.lagMinimertPerson
import no.nav.familie.ba.sak.integrasjoner.sanity.hentSanityBegrunnelser
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StandardbegrunnelseTest {

    private val behandling = lagBehandling()
    private val søker = tilfeldigPerson(personType = PersonType.SØKER)
    private val barn = tilfeldigPerson(personType = PersonType.BARN)
    private val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
        type = Vedtaksperiodetype.UTBETALING,
        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
    )
    private val vilkårsvurdering =
        lagVilkårsvurdering(søker.aktør, lagBehandling(), Resultat.OPPFYLT)
    val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

    private val aktørerMedUtbetaling = listOf(søker.aktør, barn.aktør)

    private val sanityBegrunnelser = hentSanityBegrunnelser()

    @Test
    fun `Oppfyller vilkår skal gi true`() {
        assertTrue(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Annen periode type skal gi false`() {
        assertFalse(
            Standardbegrunnelse.OPPHØR_UTVANDRET
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Har ikke barn med seksårsdag skal gi false`() {
        assertFalse(
            Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Har barn med seksårsdag skal gi true`() {
        val minimertePersoner =
            listOf(
                lagMinimertPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(6)),
                lagMinimertPerson(type = PersonType.SØKER),
            )

        assertTrue(
            Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = minimertePersoner,
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Har sats endring skal gi true`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 9, 1),
            type = Vedtaksperiodetype.UTBETALING,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        assertTrue(
            Standardbegrunnelse.INNVILGET_SATSENDRING
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = vedtaksperiodeMedBegrunnelserSatsEndring.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Har ikke sats endring skal gi false`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 8, 1),
            type = Vedtaksperiodetype.UTBETALING,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        assertFalse(
            Standardbegrunnelse.INNVILGET_SATSENDRING
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = vedtaksperiodeMedBegrunnelserSatsEndring.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Oppfyller ikke vilkår for person skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(
            Standardbegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Oppfyller vilkår for person skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker)

        assertTrue(
            Standardbegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Oppfyller etter endringsperiode skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertTrue(
            Standardbegrunnelse.ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    minimertVedtaksperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
                        type = Vedtaksperiodetype.UTBETALING,
                        fom = LocalDate.of(2021, 10, 1),
                        tom = LocalDate.of(2021, 10, 31)
                    ).tilMinimertVedtaksperiode(),
                    minimerteEndredeUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            prosent = BigDecimal.ZERO,
                            behandlingId = behandling.id,
                            person = barn,
                            fom = YearMonth.of(2021, 6),
                            tom = YearMonth.of(2021, 9),
                            årsak = Årsak.DELT_BOSTED
                        )
                    ).map { it.tilMinimertEndretUtbetalingAndel() },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Oppfyller ikke etter endringsperiode skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(
            Standardbegrunnelse.ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES
                .triggesForPeriode(
                    erIngenOverlappVedtaksperiodeToggelPå = false,
                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    minimertVedtaksperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
                        type = Vedtaksperiodetype.UTBETALING,
                        fom = LocalDate.of(2021, 10, 1),
                        tom = LocalDate.of(2021, 10, 31)
                    ).tilMinimertVedtaksperiode(),
                    minimerteEndredeUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            prosent = BigDecimal.ZERO,
                            behandlingId = behandling.id,
                            person = barn,
                            fom = YearMonth.of(2021, 10),
                            tom = YearMonth.of(2021, 10),
                            årsak = Årsak.DELT_BOSTED
                        )
                    ).map { it.tilMinimertEndretUtbetalingAndel() },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    utvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
                )
        )
    }

    @Test
    fun `Oppfyller skal utbetales gir false`() {
        assertFalse(
            lagEndretUtbetalingAndel(prosent = BigDecimal.ZERO, person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = TriggesAv(endretUtbetalingSkalUtbetales = true),
                )
        )

        assertFalse(
            lagEndretUtbetalingAndel(prosent = BigDecimal.valueOf(100), person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = TriggesAv(endretUtbetalingSkalUtbetales = false),
                )
        )
    }

    @Test
    fun `Oppfyller skal utbetales gir true`() {
        assertTrue(
            lagEndretUtbetalingAndel(prosent = BigDecimal.ZERO, person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = TriggesAv(endretUtbetalingSkalUtbetales = false),
                )
        )

        assertTrue(
            lagEndretUtbetalingAndel(prosent = BigDecimal.valueOf(100), person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = TriggesAv(endretUtbetalingSkalUtbetales = true),
                )
        )
    }

    @Test
    fun `Alle begrunnelser er unike`() {
        val vedtakBegrunnelser = Standardbegrunnelse.values().groupBy { it.sanityApiNavn }
        assertEquals(vedtakBegrunnelser.size, Standardbegrunnelse.values().size)
    }

    @Test
    fun `Alle api-navn inkluderer vedtaksbegrunnelsestypen`() {
        Standardbegrunnelse.values().groupBy { it.vedtakBegrunnelseType }
            .forEach { (vedtakBegrunnelseType, standardbegrunnelser) ->
                standardbegrunnelser.forEach {
                    val prefix = when (vedtakBegrunnelseType) {
                        VedtakBegrunnelseType.INNVILGET -> "innvilget"
                        VedtakBegrunnelseType.REDUKSJON -> "reduksjon"
                        VedtakBegrunnelseType.AVSLAG -> "avslag"
                        VedtakBegrunnelseType.OPPHØR -> "opphor"
                        VedtakBegrunnelseType.FORTSATT_INNVILGET -> "fortsattInnvilget"
                        VedtakBegrunnelseType.ENDRET_UTBETALING -> "endretUtbetaling"
                        VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> "etterEndretUtbetaling"
                    }
                    assertEquals(prefix, it.sanityApiNavn.substring(0, prefix.length), "Forventer $prefix for $it")
                    assertTrue(
                        it.sanityApiNavn.substring(prefix.length).startsWithUppercaseLetter(),
                        "Forventer stor forbokstav etter prefix '$prefix' på $it"
                    )
                }
            }
    }

    private fun String.startsWithUppercaseLetter(): Boolean {
        return this.matches(Regex("[A-Z]{1}.*"))
    }
}
