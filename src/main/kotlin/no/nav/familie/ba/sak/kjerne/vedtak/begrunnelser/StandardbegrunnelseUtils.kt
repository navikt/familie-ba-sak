package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.fomErPåSatsendring
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.MinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.harPersonerSomManglerOpplysninger
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.slf4j.LoggerFactory
import java.time.LocalDate

fun VedtakBegrunnelseSpesifikasjon.triggesForPeriode(
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    minimertePersonResultater: List<MinimertPersonResultat>,
    persongrunnlag: PersonopplysningGrunnlag,
    aktørerMedUtbetaling: List<Aktør>,
    triggesAv: TriggesAv,
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList(),
    andelerTilkjentYtelse: List<AndelTilkjentYtelse> = emptyList(),
): Boolean {

    val aktuellePersoner = persongrunnlag.personer
        .filter { person -> triggesAv.personTyper.contains(person.type) }
        .filter { person ->
            if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET) {
                aktørerMedUtbetaling.contains(person.aktør) || person.type == PersonType.SØKER
            } else true
        }

    val erEtterEndretPeriode = erEtterEndretPeriodeAvSammeÅrsak(
        endretUtbetalingAndeler,
        utvidetVedtaksperiodeMedBegrunnelser,
        aktuellePersoner,
        triggesAv
    )
    val ytelseTyperForPeriode = utvidetVedtaksperiodeMedBegrunnelser
        .utbetalingsperiodeDetaljer.map { it.ytelseType }

    val begrunnelseErRiktigType =
        utvidetVedtaksperiodeMedBegrunnelser.type.tillatteBegrunnelsestyper.contains(this.vedtakBegrunnelseType)
    return when {
        !triggesAv.valgbar -> false
        !begrunnelseErRiktigType -> false

        triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) -> this.periodeErOppyltForYtelseType(
            ytelseType = if (triggesAv.småbarnstillegg) YtelseType.SMÅBARNSTILLEGG else YtelseType.UTVIDET_BARNETRYGD,
            ytelseTyperForPeriode = ytelseTyperForPeriode,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            fomForPeriode = utvidetVedtaksperiodeMedBegrunnelser.fom
        )
        triggesAv.personerManglerOpplysninger -> minimertePersonResultater.harPersonerSomManglerOpplysninger()
        triggesAv.barnMedSeksårsdag ->
            persongrunnlag.harBarnMedSeksårsdagPåFom(utvidetVedtaksperiodeMedBegrunnelser.fom)
        triggesAv.satsendring -> fomErPåSatsendring(utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN)

        triggesAv.erEndret() ->
            erEtterEndretPeriode &&
                triggesAv.etterEndretUtbetaling &&
                utvidetVedtaksperiodeMedBegrunnelser.type != Vedtaksperiodetype.ENDRET_UTBETALING

        else -> VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = minimertePersonResultater,
            vedtaksperiode = Periode(
                fom = utvidetVedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                tom = utvidetVedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = this.vedtakBegrunnelseType,
            aktuellePersonerForVedtaksperiode = aktuellePersoner.map { it.tilMinimertPerson() },
            triggesAv = triggesAv,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse,
                utvidetVedtaksperiodeMedBegrunnelser.fom
            )
        ).isNotEmpty()
    }
}

private fun erEtterEndretPeriodeAvSammeÅrsak(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
    aktuellePersoner: List<Person>,
    triggesAv: TriggesAv
) = endretUtbetalingAndeler.any { endretUtbetalingAndel ->
    endretUtbetalingAndel.tom!!.sisteDagIInneværendeMåned()
        .erDagenFør(utvidetVedtaksperiodeMedBegrunnelser.fom) &&
        aktuellePersoner.any { person -> person.aktør == endretUtbetalingAndel.person?.aktør } &&
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
fun VedtakBegrunnelseSpesifikasjon.erTilknyttetVilkår(sanityBegrunnelser: List<SanityBegrunnelse>): Boolean =
    !this.tilSanityBegrunnelse(sanityBegrunnelser)?.vilkaar.isNullOrEmpty()

fun VedtakBegrunnelseSpesifikasjon.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser.type.tillatteBegrunnelsestyper.contains(this.vedtakBegrunnelseType)) {
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

fun VedtakBegrunnelseSpesifikasjon.periodeErOppyltForYtelseType(
    ytelseType: YtelseType,
    ytelseTyperForPeriode: List<YtelseType>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    fomForPeriode: LocalDate?,
): Boolean {
    return when (this.vedtakBegrunnelseType) {
        VedtakBegrunnelseType.INNVILGET -> ytelseTyperForPeriode.contains(ytelseType)
        VedtakBegrunnelseType.REDUKSJON -> !ytelseTyperForPeriode.contains(ytelseType) &&
            ytelseOppfyltForrigeMåned(ytelseType, andelerTilkjentYtelse, fomForPeriode)
        else -> false
    }
}

private fun ytelseOppfyltForrigeMåned(
    ytelseType: YtelseType,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    fomForPeriode: LocalDate?
) = andelerTilkjentYtelse
    .filter { it.stønadTom.sisteDagIInneværendeMåned().erDagenFør(fomForPeriode) }
    .any { it.type == ytelseType }
