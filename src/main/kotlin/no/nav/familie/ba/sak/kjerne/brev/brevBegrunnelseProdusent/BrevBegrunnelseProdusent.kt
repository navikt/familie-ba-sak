package no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.integrasjoner.pdl.logger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.erBetaltUtvidetIPeriode
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.erNullPgaDifferanseberegningEllerDeltBosted
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.finnBarnMedAlleredeUtbetalt
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.finnUtvidetAndelerIDennePerioden
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.finnUtvidetAndelerIForrigePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.erAvslagUregistrerteBarnBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.BegrunnelseGrunnlagForPeriodeMedOpphør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.EndretUtbetalingAndelForVedtaksperiodeDeltBosted
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.IEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.erOppfyltForBarn
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate

data class GrunnlagForBegrunnelse(
    val behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    val behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    val sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    val sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    val nåDato: LocalDate,
)

fun Standardbegrunnelse.lagBrevBegrunnelse(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    grunnlag: GrunnlagForBegrunnelse,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
): List<BegrunnelseData> {
    val sanityBegrunnelse = hentSanityBegrunnelse(grunnlag)

    val personerGjeldeneForBegrunnelse =
        vedtaksperiode
            .hentGyldigeBegrunnelserPerPerson(grunnlag)
            .mapNotNull { (person, begrunnelserPåPerson) -> person.takeIf { this in begrunnelserPåPerson } }

    val gjelderSøker = gjelderBegrunnelseSøker(personerGjeldeneForBegrunnelse)

    val månedOgÅrBegrunnelsenGjelderFor = vedtaksperiode.hentMånedOgÅrForBegrunnelse()

    return if (this.kanDelesOpp) {
        delOppBegrunnelsenPåAvtaletidspunkt(
            sanityBegrunnelse = sanityBegrunnelse,
            personerGjeldeneForBegrunnelse = personerGjeldeneForBegrunnelse,
            begrunnelseGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
            gjelderSøker = gjelderSøker,
            månedOgÅrBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
            grunnlag = grunnlag,
        )
    } else {
        lagEnkeltBegrunnelse(
            begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
            personerGjeldeneForBegrunnelse = personerGjeldeneForBegrunnelse,
            gjelderSøker = gjelderSøker,
            sanityBegrunnelse = sanityBegrunnelse,
            grunnlag = grunnlag,
            månedOgÅrBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
            vedtaksperiode = vedtaksperiode,
        )
    }
}

private fun Standardbegrunnelse.lagEnkeltBegrunnelse(
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    personerGjeldeneForBegrunnelse: List<Person>,
    gjelderSøker: Boolean,
    sanityBegrunnelse: ISanityBegrunnelse,
    grunnlag: GrunnlagForBegrunnelse,
    månedOgÅrBegrunnelsenGjelderFor: String?,
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
): List<BegrunnelseData> {
    val grunnlagForPersonerIBegrunnelsen =
        begrunnelsesGrunnlagPerPerson.filtrerPåErPersonIBegrunnelse(personerGjeldeneForBegrunnelse)

    val beløp =
        hentBeløp(
            gjelderSøker = gjelderSøker,
            begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
            grunnlagForPersonerIBegrunnelsen = grunnlagForPersonerIBegrunnelsen,
        )

    val endreteUtbetalingsAndelerForBegrunnelse =
        sanityBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(grunnlagForPersonerIBegrunnelsen)

    val søknadstidspunktEndretUtbetaling =
        endreteUtbetalingsAndelerForBegrunnelse
            .hentSøknadstidspunkt(sanityBegrunnelse)

    val barnasFødselsdatoer =
        sanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            grunnlag = grunnlag,
            gjelderSøker = gjelderSøker,
            personerIBegrunnelse = personerGjeldeneForBegrunnelse,
            begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
        )

    val antallBarn =
        hentAntallBarnForBegrunnelse(
            begrunnelse = this,
            grunnlag = grunnlag,
            barnasFødselsdatoer = barnasFødselsdatoer,
            gjelderSøker = gjelderSøker,
            antallBarnGjeldendeForBegrunnelse = personerGjeldeneForBegrunnelse.filter { it.type == PersonType.BARN }.size,
        )

    sanityBegrunnelse.validerBrevbegrunnelse(
        gjelderSøker,
        barnasFødselsdatoer,
    )

    return listOf(
        BegrunnelseData(
            gjelderSoker = gjelderSøker,
            barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
            antallBarn = antallBarn,
            maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
            maalform =
                grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform
                    .tilSanityFormat(),
            apiNavn = this.sanityApiNavn,
            belop = Utils.formaterBeløp(beløp),
            soknadstidspunkt = søknadstidspunktEndretUtbetaling?.tilKortString() ?: "",
            avtaletidspunktDeltBosted = "",
            sokersRettTilUtvidet =
                hentSøkersRettTilUtvidet(
                    utvidetUtbetalingsdetaljer =
                        hentUtvidetAndelerIPeriode(
                            begrunnelsesGrunnlagPerPerson,
                        ),
                ).tilSanityFormat(),
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        ),
    )
}

private fun List<IEndretUtbetalingAndelForVedtaksperiode>.hentSøknadstidspunkt(
    sanityBegrunnelse: ISanityBegrunnelse,
) = sortedBy { it.søknadstidspunkt }
    .firstOrNull { sanityBegrunnelse is SanityBegrunnelse && it.årsak in sanityBegrunnelse.endringsaarsaker }
    ?.søknadstidspunkt

private fun Standardbegrunnelse.delOppBegrunnelsenPåAvtaletidspunkt(
    sanityBegrunnelse: ISanityBegrunnelse,
    personerGjeldeneForBegrunnelse: List<Person>,
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    gjelderSøker: Boolean,
    månedOgÅrBegrunnelsenGjelderFor: String?,
    grunnlag: GrunnlagForBegrunnelse,
): List<BegrunnelseData> {
    val begrunnelseGrunnlagForBarnIBegrunnelse =
        personerGjeldeneForBegrunnelse
            .filter { it.type == PersonType.BARN }
            .mapNotNull { begrunnelseGrunnlagPerPerson[it]?.dennePerioden }

    val gruppertPåAvtaletidspunkt =
        begrunnelseGrunnlagForBarnIBegrunnelse
            .mapNotNull {
                if (it.endretUtbetalingAndel is EndretUtbetalingAndelForVedtaksperiodeDeltBosted) {
                    Pair(it.endretUtbetalingAndel.avtaletidspunktDeltBosted, it)
                } else {
                    null
                }
            }.sortedBy { it.first }
            .groupBy({ it.first }, { it.second })

    return gruppertPåAvtaletidspunkt.map { (avtaletidspunktDeltBosted, begrunnelseGrunnlag) ->
        val barnaTilhørendeAvtaletidspunktFødselsdatoer = begrunnelseGrunnlag.map { it.person.fødselsdato }

        val beløpUtbetaltForBarnTilhørendeAvtaletidspunkt =
            begrunnelseGrunnlag.sumOf { grunnlagForPersonIPeriode ->
                grunnlagForPersonIPeriode.andeler.sumOf { it.kalkulertUtbetalingsbeløp }
            }

        val søknadstidspunkt =
            begrunnelseGrunnlag
                .mapNotNull { it.endretUtbetalingAndel }
                .hentSøknadstidspunkt(sanityBegrunnelse)

        sanityBegrunnelse.validerBrevbegrunnelse(
            gjelderSøker,
            barnaTilhørendeAvtaletidspunktFødselsdatoer,
        )

        BegrunnelseData(
            gjelderSoker = gjelderSøker,
            barnasFodselsdatoer = barnaTilhørendeAvtaletidspunktFødselsdatoer.tilBrevTekst(),
            antallBarn = barnaTilhørendeAvtaletidspunktFødselsdatoer.size,
            maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
            maalform =
                grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform
                    .tilSanityFormat(),
            apiNavn = this.sanityApiNavn,
            belop = Utils.formaterBeløp(beløpUtbetaltForBarnTilhørendeAvtaletidspunkt),
            soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted.tilKortString(),
            sokersRettTilUtvidet =
                hentSøkersRettTilUtvidet(
                    utvidetUtbetalingsdetaljer =
                        hentUtvidetAndelerIPeriode(
                            begrunnelseGrunnlagPerPerson,
                        ),
                ).tilSanityFormat(),
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        )
    }
}

private fun hentUtvidetAndelerIPeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.values
        .flatMap { it.dennePerioden.andeler }
        .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }

fun IVedtakBegrunnelse.hentSanityBegrunnelse(grunnlag: GrunnlagForBegrunnelse) =
    when (this) {
        is EØSStandardbegrunnelse -> grunnlag.sanityEØSBegrunnelser[this]
        is Standardbegrunnelse -> grunnlag.sanityBegrunnelser[this]
    } ?: throw Feil("Fant ikke tilsvarende sanitybegrunnelse for $this")

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.finnBarnMedUtbetaling() =
    this
        .filterKeys { it.type == PersonType.BARN }
        .filterValues { grunnlag ->
            val erUtbetalingsbeløpStørreEnnNull = grunnlag.dennePerioden.andeler.any { it.kalkulertUtbetalingsbeløp > 0 }

            erUtbetalingsbeløpStørreEnnNull || erNullPgaDifferanseberegningEllerDeltBosted(grunnlag)
        }.keys

private fun hentPersonerMedAndelIForrigePeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            !begrunnelseGrunnlagForPersonIPeriode.forrigePeriode
                ?.andeler
                ?.toList()
                .isNullOrEmpty()
        }.keys

private fun hentPersonerMedAndelHøyereEnn0IForrigePeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            !begrunnelseGrunnlagForPersonIPeriode.forrigePeriode
                ?.andeler
                ?.filter { it.kalkulertUtbetalingsbeløp > 0 }
                ?.toList()
                .isNullOrEmpty()
        }.keys

private fun hentPersonerMedDeltBostedIForrigePeriodeMenIkkeDenne(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            val deltBostedIForrigePeriode = begrunnelseGrunnlagForPersonIPeriode.forrigePeriode?.endretUtbetalingAndel?.prosent == BigDecimal.valueOf(50)
            val deltBostedIDennePerioden = begrunnelseGrunnlagForPersonIPeriode.dennePerioden?.endretUtbetalingAndel?.prosent == BigDecimal.valueOf(50)

            deltBostedIForrigePeriode && !deltBostedIDennePerioden
        }.keys

private fun hentPersonerSomHarHattEndringIEndretUtbetalingAndelIDennePerioden(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            val endretUtbetalingAndelIForrigePeriode = begrunnelseGrunnlagForPersonIPeriode.forrigePeriode?.endretUtbetalingAndel
            val endretUtbetalingAndelIDennePeriode = begrunnelseGrunnlagForPersonIPeriode.dennePerioden.endretUtbetalingAndel

            endretUtbetalingAndelIForrigePeriode != endretUtbetalingAndelIDennePeriode
        }.keys

private fun hentPersonerMistetUtbetalingFraForrigeBehandling(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            begrunnelseGrunnlagForPersonIPeriode is BegrunnelseGrunnlagForPeriodeMedOpphør &&
                !begrunnelseGrunnlagForPersonIPeriode.sammePeriodeForrigeBehandling
                    ?.andeler
                    ?.toList()
                    .isNullOrEmpty()
        }.keys

private fun gjelderBegrunnelseSøker(personerGjeldeneForBegrunnelse: List<Person>) = personerGjeldeneForBegrunnelse.any { it.type == PersonType.SØKER }

fun ISanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    personerIBegrunnelse: List<Person>,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
): List<LocalDate> {
    val barnPåBegrunnelse = personerIBegrunnelse.filter { it.type == PersonType.BARN }
    val barnMedUtbetaling =
        begrunnelsesGrunnlagPerPerson
            .finnBarnMedUtbetaling()
            .ifEmpty {
                val erBetaltUtvidetIPeriode = begrunnelsesGrunnlagPerPerson.erBetaltUtvidetIPeriode()
                when {
                    erBetaltUtvidetIPeriode -> begrunnelsesGrunnlagPerPerson.finnBarnMedAlleredeUtbetalt()
                    else -> emptySet()
                }
            }
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn

    val barnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.barna

    val barnMedOppfylteVilkår = hentBarnMedOppfylteVilkår(begrunnelsesGrunnlagPerPerson)
    val barnMedUtbetalingIForrigeperiode =
        hentPersonerMedAndelIForrigePeriode(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }
    val barnMedUtbetalingHøyereEnn0IForrigeperiode = hentPersonerMedAndelHøyereEnn0IForrigePeriode(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }
    val barnMistetUtbetalingFraForrigeBehandling =
        hentPersonerMistetUtbetalingFraForrigeBehandling(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }

    val barnSomHaddeDeltBostedIForrigePeriodeMenIkkeDenne = hentPersonerMedDeltBostedIForrigePeriodeMenIkkeDenne(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }
    val barnSomNåFårUtbetalingIPeriode = barnMedUtbetaling - barnMedUtbetalingHøyereEnn0IForrigeperiode

    val barnMedNullutbetalingForrigePeriodeGrunnetEndretUtbetaling =
        hentBarnMedNullutbetalingForrigePeriodeGrunnetEndretUtbetaling(begrunnelsesGrunnlagPerPerson)

    return when {
        this.erAvslagUregistrerteBarnBegrunnelse() -> {
            uregistrerteBarnPåBehandlingen.mapNotNull { it.fødselsdato }
        }

        this.gjelderFinnmarkstillegg || this.gjelderSvalbardtillegg -> {
            barnPåBegrunnelse.map { it.fødselsdato }
        }

        gjelderSøker && !this.gjelderEtterEndretUtbetaling && !this.gjelderEndretutbetaling -> {
            when (this.periodeResultat) {
                SanityPeriodeResultat.IKKE_INNVILGET -> {
                    hentRelevanteBarnVedIkkeInnvilget(
                        begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
                        grunnlag = grunnlag,
                        barnPåBehandlingen = barnPåBehandlingen,
                        uregistrerteBarnPåBehandlingen = uregistrerteBarnPåBehandlingen,
                        barnMedUtbetalingIForrigeperiode = barnMedUtbetalingIForrigeperiode,
                        barnMedOppfylteVilkår = barnMedOppfylteVilkår,
                        barnMistetUtbetalingFraForrigeBehandling = barnMistetUtbetalingFraForrigeBehandling,
                        barnMedNullutbetalingForrigePeriode = barnMedNullutbetalingForrigePeriodeGrunnetEndretUtbetaling,
                        vedtaksperiode = vedtaksperiode,
                    )
                }

                else -> {
                    (barnMedUtbetaling + barnPåBegrunnelse).toSet().map { it.fødselsdato }
                }
            }
        }

        gjelderSøker && this.gjelderEtterEndretUtbetaling -> {
            barnPåBegrunnelse
                .ifEmpty { barnMedUtbetaling }
                .map { it.fødselsdato }
        }

        erDeltBostedOgInnvilgetEllerØkningOgSkalUtbetales(this) -> {
            hentBarnSomSkalUtbetalesVedDeltBosted(begrunnelsesGrunnlagPerPerson).keys.map { it.fødselsdato }
        }

        erEtterEndretUtbetalingOgErIkkeAlleredeUtbetalt(this) -> {
            (barnSomHaddeDeltBostedIForrigePeriodeMenIkkeDenne + barnSomNåFårUtbetalingIPeriode).distinct().map { it.fødselsdato }
        }

        this.gjelderEndretutbetaling -> {
            barnPåBegrunnelse
                .filter { hentPersonerSomHarHattEndringIEndretUtbetalingAndelIDennePerioden(begrunnelsesGrunnlagPerPerson).contains(it) }
                .ifEmpty { barnPåBegrunnelse }
                .map { it.fødselsdato }
        }

        else -> {
            barnPåBegrunnelse.map { it.fødselsdato }
        }
    }
}

private fun ISanityBegrunnelse.hentRelevanteBarnVedIkkeInnvilget(
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlag: GrunnlagForBegrunnelse,
    barnPåBehandlingen: List<Person>,
    uregistrerteBarnPåBehandlingen: List<BarnMedOpplysninger>,
    barnMedUtbetalingIForrigeperiode: List<Person>,
    barnMedOppfylteVilkår: List<Person>,
    barnMistetUtbetalingFraForrigeBehandling: List<Person>,
    barnMedNullutbetalingForrigePeriode: Set<Person>,
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
): List<LocalDate> {
    val erAvslagPåSøker = erEksplisittAvslagPåSøker(begrunnelsesGrunnlagPerPerson, grunnlag)

    val relevanteBarn =
        if (erAvslagPåSøker) {
            val personerFramstiltKravFor = grunnlag.behandlingsGrunnlagForVedtaksperioder.personerFremstiltKravFor
            val barnDetErFramstiltKravFor = barnPåBehandlingen.filter { it.aktør in personerFramstiltKravFor }.ifEmpty { barnPåBehandlingen }

            barnDetErFramstiltKravFor
                .filter { it.fødselsdato < (vedtaksperiode.tom ?: TIDENES_ENDE) }
                .ifEmpty { barnPåBehandlingen }
                .map { it.fødselsdato } + uregistrerteBarnPåBehandlingen.mapNotNull { it.fødselsdato }
        } else {
            val alleRelevanteBarn =
                (
                    barnMedUtbetalingIForrigeperiode +
                        barnMedOppfylteVilkår +
                        barnMistetUtbetalingFraForrigeBehandling
                ).toSet()

            val alleRelevanteBarnMedUtbetalingForrigePeriode = alleRelevanteBarn.minus(barnMedNullutbetalingForrigePeriode)

            alleRelevanteBarnMedUtbetalingForrigePeriode
                .ifEmpty { alleRelevanteBarn }
                .map { it.fødselsdato }
        }

    return relevanteBarn
}

private fun hentBarnMedNullutbetalingForrigePeriodeGrunnetEndretUtbetaling(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filterKeys { it.type == PersonType.BARN }
        .filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
            val andelerTilkjentYtelse = begrunnelseGrunnlagForPersonIPeriode.forrigePeriode?.andeler?.toList()
            val endretUtbetalingAndel = begrunnelseGrunnlagForPersonIPeriode.forrigePeriode?.endretUtbetalingAndel
            val endretUtbetalingAndelErSattTil0 = endretUtbetalingAndel?.prosent == BigDecimal.ZERO

            andelerTilkjentYtelse?.isNotEmpty() == true &&
                andelerTilkjentYtelse.none { it.kalkulertUtbetalingsbeløp > 0 } &&
                endretUtbetalingAndelErSattTil0
        }.keys

private fun hentBarnSomSkalUtbetalesVedDeltBosted(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.filter { (person, begrunnelseGrunnlag) ->
        val endretUtbetalingAndelIPeriode = begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel
        val deltBostedIVilkårsvurderingIPeriode =
            begrunnelseGrunnlag.dennePerioden.vilkårResultater.any {
                UtdypendeVilkårsvurdering.DELT_BOSTED in it.utdypendeVilkårsvurderinger
            }
        val andelerIPeriode = begrunnelseGrunnlag.dennePerioden.andeler
        val erDeltBostedIVilkårsvurderingMedUtbetalingIPeriode = deltBostedIVilkårsvurderingIPeriode && andelerIPeriode.any { it.prosent != BigDecimal.ZERO }

        val sumAndelerDennePerioden = andelerIPeriode.sumOf { it.kalkulertUtbetalingsbeløp }
        val sumAndelerForrigePeriode = begrunnelseGrunnlag.forrigePeriode?.andeler?.sumOf { it.kalkulertUtbetalingsbeløp } ?: 0

        val utvidetAndelerDennePerioden = begrunnelsesGrunnlagPerPerson.finnUtvidetAndelerIDennePerioden()
        val utvidetAndelerForrigePeriode = begrunnelsesGrunnlagPerPerson.finnUtvidetAndelerIForrigePeriode()
        val deltUtvidetSumDennePerioden = utvidetAndelerDennePerioden.filter { it.prosent == BigDecimal.valueOf(50) }.sumOf { it.kalkulertUtbetalingsbeløp }
        val deltUtvidetSumForrigePeriode = utvidetAndelerForrigePeriode.filter { it.prosent == BigDecimal.valueOf(50) }.sumOf { it.kalkulertUtbetalingsbeløp }

        (
            (
                endretUtbetalingAndelIPeriode?.årsak == Årsak.DELT_BOSTED &&
                    endretUtbetalingAndelIPeriode.prosent != BigDecimal.ZERO
            ) ||
                erDeltBostedIVilkårsvurderingMedUtbetalingIPeriode
        ) &&
            person.type == PersonType.BARN &&
            (sumAndelerDennePerioden != sumAndelerForrigePeriode || (deltUtvidetSumForrigePeriode != deltUtvidetSumDennePerioden))
    }

private fun erEtterEndretUtbetalingOgErIkkeAlleredeUtbetalt(sanityBegrunnelse: ISanityBegrunnelse) =
    sanityBegrunnelse.gjelderEtterEndretUtbetaling &&
        sanityBegrunnelse is SanityBegrunnelse &&
        !sanityBegrunnelse.endringsaarsaker.contains(Årsak.ALLEREDE_UTBETALT)

private fun erDeltBostedOgInnvilgetEllerØkningOgSkalUtbetales(
    sanityBegrunnelse: ISanityBegrunnelse,
): Boolean =
    sanityBegrunnelse is SanityBegrunnelse &&
        (
            (sanityBegrunnelse.gjelderEndretutbetaling && sanityBegrunnelse.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger == EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES) ||
                sanityBegrunnelse.gjelderEtterEndretUtbetaling
        ) &&
        sanityBegrunnelse.endringsaarsaker.contains(Årsak.DELT_BOSTED) &&
        sanityBegrunnelse.periodeResultat == SanityPeriodeResultat.INNVILGET_ELLER_ØKNING

private fun ISanityBegrunnelse.erEksplisittAvslagPåSøker(
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlag: GrunnlagForBegrunnelse,
): Boolean {
    val explisitteAvslagsvilkårForSøker =
        begrunnelsesGrunnlagPerPerson[grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker]
            ?.dennePerioden
            ?.eksplisitteAvslagForPerson ?: emptyList()

    return explisitteAvslagsvilkårForSøker.any {
        this.begrunnelseTypeForPerson == VedtakBegrunnelseType.AVSLAG &&
            it.vilkårType in this.vilkår
    }
}

private fun hentBarnMedOppfylteVilkår(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson
        .filterKeys { it.type == PersonType.BARN }
        .filter {
            it.value.dennePerioden.vilkårResultater
                .erOppfyltForBarn()
        }.map { it.key }

fun hentAntallBarnForBegrunnelse(
    begrunnelse: IVedtakBegrunnelse,
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
    antallBarnGjeldendeForBegrunnelse: Int,
): Int {
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn
    val erAvslagUregistrerteBarn = begrunnelse.erAvslagUregistrerteBarnBegrunnelse()

    return when {
        erAvslagUregistrerteBarn -> {
            uregistrerteBarnPåBehandlingen.size
        }

        gjelderSøker &&
            begrunnelse.vedtakBegrunnelseType in
            listOf(
                VedtakBegrunnelseType.AVSLAG,
                VedtakBegrunnelseType.OPPHØR,
            )
        -> {
            antallBarnGjeldendeForBegrunnelse
        }

        else -> {
            barnasFødselsdatoer.size
        }
    }
}

fun VedtaksperiodeMedBegrunnelser.hentMånedOgÅrForBegrunnelse(): String? =
    if (this.fom == null || fom == TIDENES_MORGEN) {
        null
    } else {
        fom.forrigeMåned().tilMånedÅr()
    }

private fun hentBeløp(
    gjelderSøker: Boolean,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    grunnlagForPersonerIBegrunnelsen: Map<Person, IBegrunnelseGrunnlagForPeriode>,
) = if (gjelderSøker) {
    begrunnelsesGrunnlagPerPerson.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }
} else {
    grunnlagForPersonerIBegrunnelsen.values.sumOf { it.dennePerioden.andeler.sumOf { andeler -> andeler.kalkulertUtbetalingsbeløp } }
}

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.filtrerPåErPersonIBegrunnelse(
    personerGjeldeneForBegrunnelse: List<Person>,
) = this.filter { (k, _) -> k in personerGjeldeneForBegrunnelse }

fun ISanityBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
    grunnlagForPersonerIBegrunnelsen: Map<Person, IBegrunnelseGrunnlagForPeriode>,
) = when {
    this.gjelderEtterEndretUtbetaling -> {
        grunnlagForPersonerIBegrunnelsen.mapNotNull { it.value.forrigePeriode?.endretUtbetalingAndel }
    }

    this.gjelderEndretutbetaling -> {
        grunnlagForPersonerIBegrunnelsen.mapNotNull { it.value.dennePerioden.endretUtbetalingAndel }
    }

    else -> {
        emptyList()
    }
}

private fun ISanityBegrunnelse.validerBrevbegrunnelse(
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
) {
    if (!gjelderSøker && barnasFødselsdatoer.isEmpty() && !this.gjelderSatsendring && !this.erAvslagUregistrerteBarnBegrunnelse()) {
        logger.warn("Ingen personer på brevbegrunnelse ${this.apiNavn}")
        throw BrevBegrunnelseFeil("Begrunnelsen ${this.navnISystem} er ikke gyldig for denne perioden. Kontakt team BAKS hvis du mener det er feil.")
    }
}

private fun hentSøkersRettTilUtvidet(utvidetUtbetalingsdetaljer: List<AndelForVedtaksbegrunnelse>): SøkersRettTilUtvidet =
    when {
        utvidetUtbetalingsdetaljer.any { it.prosent > BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_FÅR_UTVIDET
        utvidetUtbetalingsdetaljer.isNotEmpty() && utvidetUtbetalingsdetaljer.all { it.prosent == BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_HAR_RETT_MEN_FÅR_IKKE
        else -> SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT
    }

enum class SøkersRettTilUtvidet {
    SØKER_FÅR_UTVIDET,
    SØKER_HAR_RETT_MEN_FÅR_IKKE,
    SØKER_HAR_IKKE_RETT, ;

    fun tilSanityFormat() =
        when (this) {
            SØKER_FÅR_UTVIDET -> "sokerFaarUtvidet"
            SØKER_HAR_RETT_MEN_FÅR_IKKE -> "sokerHarRettMenFaarIkke"
            SØKER_HAR_IKKE_RETT -> "sokerHarIkkeRett"
        }
}

fun ISanityBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this.apiNavn in
        setOf(
            Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN.sanityApiNavn,
            EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN.sanityApiNavn,
        )

class BrevBegrunnelseFeil(
    melding: String,
) : FunksjonellFeil(melding)
