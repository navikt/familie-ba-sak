package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.fomErPåSatsendring
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.harBarnMedSeksårsdagPåFom
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun VedtakBegrunnelseSpesifikasjon.triggesForPeriode(
    minimertVedtaksperiode: MinimertVedtaksperiode,
    minimertePersonResultater: List<MinimertRestPersonResultat>,
    minimertePersoner: List<MinimertPerson>,
    aktørIderMedUtbetaling: List<String>,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel> = emptyList(),
    sanityBegrunnelser: List<SanityBegrunnelse>,
    erFørsteVedtaksperiodePåFagsak: Boolean,
    ytelserForSøkerForrigeMåned: List<YtelseType>,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
): Boolean {

    val triggesAv = this.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv() ?: return false

    val aktuellePersoner = minimertePersoner
        .filter { person -> triggesAv.personTyper.contains(person.type) }
        .filter { person ->
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                aktørIderMedUtbetaling.contains(person.aktørId) || person.type == PersonType.SØKER
            } else true
        }

    val ytelseTyperForPeriode = minimertVedtaksperiode.ytelseTyperForPeriode

    fun hentPersonerForUtgjørendeVilkår() = hentPersonerForAlleUtgjørendeVilkår(
        minimertePersonResultater = minimertePersonResultater,
        vedtaksperiode = Periode(
            fom = minimertVedtaksperiode.fom ?: TIDENES_MORGEN,
            tom = minimertVedtaksperiode.tom ?: TIDENES_ENDE
        ),
        oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
        aktuellePersonerForVedtaksperiode = aktuellePersoner.map { it.tilMinimertRestPerson() },
        triggesAv = triggesAv,
        erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
    )

    return when {
        !triggesAv.valgbar -> false

        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) && !triggesAv.erEndret() -> this.vedtakBegrunnelseType.periodeErOppyltForYtelseType(
            ytelseType = if (triggesAv.småbarnstillegg) YtelseType.SMÅBARNSTILLEGG else YtelseType.UTVIDET_BARNETRYGD,
            ytelseTyperForPeriode = ytelseTyperForPeriode,
            ytelserGjeldeneForSøkerForrigeMåned = ytelserForSøkerForrigeMåned
        ) || when {
            triggesAv.vilkår.any { it != Vilkår.UTVIDET_BARNETRYGD } -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
            else -> false
        }
        triggesAv.personerManglerOpplysninger -> minimertePersonResultater.harPersonerSomManglerOpplysninger()
        triggesAv.barnMedSeksårsdag ->
            minimertePersoner.harBarnMedSeksårsdagPåFom(minimertVedtaksperiode.fom)
        triggesAv.satsendring -> fomErPåSatsendring(minimertVedtaksperiode.fom ?: TIDENES_MORGEN)

        triggesAv.etterEndretUtbetaling && minimertVedtaksperiode.type != Vedtaksperiodetype.ENDRET_UTBETALING ->
            erEtterEndretPeriodeAvSammeÅrsak(
                minimerteEndredeUtbetalingAndeler,
                minimertVedtaksperiode,
                aktuellePersoner,
                triggesAv
            )

        triggesAv.erEndret() && !triggesAv.etterEndretUtbetaling -> erEndretTriggerErOppfylt(
            triggesAv = triggesAv,
            minimerteEndredeUtbetalingAndeler = minimerteEndredeUtbetalingAndeler,
            minimertVedtaksperiode = minimertVedtaksperiode,
            utvidetScenarioForEndringsperiode = utvidetScenarioForEndringsperiode,
            erIngenOverlappVedtaksperiodeToggelPå = erIngenOverlappVedtaksperiodeToggelPå,
            ytelseTyperForPeriode = ytelseTyperForPeriode,
        )
        triggesAv.gjelderFraInnvilgelsestidspunkt -> false
        else -> hentPersonerForUtgjørendeVilkår().isNotEmpty()
    }
}

private fun erEndretTriggerErOppfylt(
    triggesAv: TriggesAv,
    minimerteEndredeUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
    ytelseTyperForPeriode: Set<YtelseType>
): Boolean {
    val endredeAndelerSomOverlapperVedtaksperiode = minimertVedtaksperiode
        .finnEndredeAndelerISammePeriode(minimerteEndredeUtbetalingAndeler)

    return (erIngenOverlappVedtaksperiodeToggelPå || minimertVedtaksperiode.type == Vedtaksperiodetype.ENDRET_UTBETALING) &&
        endredeAndelerSomOverlapperVedtaksperiode.any {
            triggesAv.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioForEndringsperiode,
                minimertEndretAndel = it,
                ytelseTyperForPeriode = ytelseTyperForPeriode,
                erIngenOverlappVedtaksperiodeToggelPå = erIngenOverlappVedtaksperiodeToggelPå,
            )
        }
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<MinimertEndretAndel>,
    minimertVedtaksperiode: MinimertVedtaksperiode,
    aktuellePersoner: List<MinimertPerson>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.månedPeriode().tom.sisteDagIInneværendeMåned()
        .erDagenFør(minimertVedtaksperiode.fom) &&
        aktuellePersoner.any { person -> person.aktørId == endretUtbetalingAndel.aktørId } &&
        triggesAv.endringsaarsaker.contains(endretUtbetalingAndel.årsak)
}

private val logger = LoggerFactory.getLogger(VedtakBegrunnelseSpesifikasjon::class.java)

fun VedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(
    sanityBegrunnelser: List<SanityBegrunnelse>
): SanityBegrunnelse? {
    val sanityBegrunnelse = sanityBegrunnelser.find { it.apiNavn == this.sanityApiNavn }
    if (sanityBegrunnelse == null) {
        logger.warn("Finner ikke begrunnelse med apinavn '${this.sanityApiNavn}' på '${this.name}' i Sanity")
    }
    return sanityBegrunnelse
}

fun List<LocalDate>.tilBrevTekst(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })

fun VedtakBegrunnelseSpesifikasjon.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
        .type
        .tillatteBegrunnelsestyper(erIngenOverlappVedtaksperiodeToggelPå)
        .contains(this.vedtakBegrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden."
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        vedtakBegrunnelseSpesifikasjon = this,
    )
}

fun VedtakBegrunnelseType.periodeErOppyltForYtelseType(
    ytelseType: YtelseType,
    ytelseTyperForPeriode: Set<YtelseType>,
    ytelserGjeldeneForSøkerForrigeMåned: List<YtelseType>
): Boolean {
    return when (this) {
        VedtakBegrunnelseType.INNVILGET -> ytelseTyperForPeriode.contains(ytelseType)
        VedtakBegrunnelseType.REDUKSJON -> !ytelseTyperForPeriode.contains(ytelseType) &&
            ytelseOppfyltForrigeMåned(ytelseType, ytelserGjeldeneForSøkerForrigeMåned)
        else -> false
    }
}

private fun ytelseOppfyltForrigeMåned(
    ytelseType: YtelseType,
    ytelserGjeldeneForSøkerForrigeMåned: List<YtelseType>,
) = ytelserGjeldeneForSøkerForrigeMåned
    .any { it == ytelseType }
