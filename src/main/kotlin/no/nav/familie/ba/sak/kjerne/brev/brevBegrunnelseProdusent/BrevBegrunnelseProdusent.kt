package no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
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
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.EndretUtbetalingAndelForVedtaksperiodeDeltBosted
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.IEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.erOppfyltForBarn
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

    val personerGjeldeneForBegrunnelse = vedtaksperiode.hentGyldigeBegrunnelserPerPerson(grunnlag)
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
): List<BegrunnelseData> {
    val grunnlagForPersonerIBegrunnelsen =
        begrunnelsesGrunnlagPerPerson.filtrerPåErPersonIBegrunnelse(personerGjeldeneForBegrunnelse)

    val beløp = hentBeløp(
        gjelderSøker = gjelderSøker,
        begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
        grunnlagForPersonerIBegrunnelsen = grunnlagForPersonerIBegrunnelsen,
    )

    val endreteUtbetalingsAndelerForBegrunnelse =
        sanityBegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(grunnlagForPersonerIBegrunnelsen)

    val søknadstidspunktEndretUtbetaling = endreteUtbetalingsAndelerForBegrunnelse
        .hentSøknadstidspunkt(sanityBegrunnelse)

    val barnasFødselsdatoer = sanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
        grunnlag = grunnlag,
        gjelderSøker = gjelderSøker,
        personerIBegrunnelse = personerGjeldeneForBegrunnelse,
        begrunnelsesGrunnlagPerPerson = begrunnelsesGrunnlagPerPerson,
    )

    val antallBarn = hentAntallBarnForBegrunnelse(
        begrunnelse = this,
        grunnlag = grunnlag,
        barnasFødselsdatoer = barnasFødselsdatoer,
        gjelderSøker = gjelderSøker,
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
            maalform = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform.tilSanityFormat(),
            apiNavn = this.sanityApiNavn,
            belop = Utils.formaterBeløp(beløp),
            soknadstidspunkt = søknadstidspunktEndretUtbetaling?.tilKortString() ?: "",
            avtaletidspunktDeltBosted = "",
            sokersRettTilUtvidet = hentSøkersRettTilUtvidet(
                utvidetUtbetalingsdetaljer = hentUtvidetAndelerIPeriode(
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
    .firstOrNull { sanityBegrunnelse is SanityBegrunnelse && it.årsak in sanityBegrunnelse.endringsaarsaker }?.søknadstidspunkt

private fun Standardbegrunnelse.delOppBegrunnelsenPåAvtaletidspunkt(
    sanityBegrunnelse: ISanityBegrunnelse,
    personerGjeldeneForBegrunnelse: List<Person>,
    begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    gjelderSøker: Boolean,
    månedOgÅrBegrunnelsenGjelderFor: String?,
    grunnlag: GrunnlagForBegrunnelse,
): List<BegrunnelseData> {
    val begrunnelseGrunnlagForBarnIBegrunnelse = personerGjeldeneForBegrunnelse
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

        val beløpUtbetaltForBarnTilhørendeAvtaletidspunkt = begrunnelseGrunnlag.sumOf { grunnlagForPersonIPeriode ->
            grunnlagForPersonIPeriode.andeler.sumOf { it.kalkulertUtbetalingsbeløp }
        }

        val søknadstidspunkt = begrunnelseGrunnlag.mapNotNull { it.endretUtbetalingAndel }
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
            maalform = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform.tilSanityFormat(),
            apiNavn = this.sanityApiNavn,
            belop = Utils.formaterBeløp(beløpUtbetaltForBarnTilhørendeAvtaletidspunkt),
            soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted.tilKortString(),
            sokersRettTilUtvidet = hentSøkersRettTilUtvidet(
                utvidetUtbetalingsdetaljer = hentUtvidetAndelerIPeriode(
                    begrunnelseGrunnlagPerPerson,
                ),
            ).tilSanityFormat(),
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        )
    }
}

private fun hentUtvidetAndelerIPeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.values.flatMap { it.dennePerioden.andeler }
        .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }

fun IVedtakBegrunnelse.hentSanityBegrunnelse(grunnlag: GrunnlagForBegrunnelse) = when (this) {
    is EØSStandardbegrunnelse -> grunnlag.sanityEØSBegrunnelser[this]
    is Standardbegrunnelse -> grunnlag.sanityBegrunnelser[this]
} ?: throw Feil("Fant ikke tilsvarende sanitybegrunnelse for $this")

private fun hentPersonerMedAndelIPeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
        begrunnelseGrunnlagForPersonIPeriode.dennePerioden.andeler.toList().isNotEmpty()
    }.keys

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.hentPersonerMedAvslagIPeriode() =
    filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
        !begrunnelseGrunnlagForPersonIPeriode.dennePerioden.eksplisitteAvslagForPerson.isNullOrEmpty()
    }.keys

private fun hentPersonerMedAndelIForrigePeriode(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.filter { (_, begrunnelseGrunnlagForPersonIPeriode) ->
        !begrunnelseGrunnlagForPersonIPeriode.forrigePeriode?.andeler?.toList().isNullOrEmpty()
    }.keys

private fun gjelderBegrunnelseSøker(personerGjeldeneForBegrunnelse: List<Person>) =
    personerGjeldeneForBegrunnelse.any { it.type == PersonType.SØKER }

fun ISanityBegrunnelse.hentBarnasFødselsdatoerForBegrunnelse(
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    personerIBegrunnelse: List<Person>,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
): List<LocalDate> {
    val barnPåBegrunnelse = personerIBegrunnelse.filter { it.type == PersonType.BARN }
    val barnMedUtbetaling =
        hentPersonerMedAndelIPeriode(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn
    val barnMedEksplisitteAvslag =
        begrunnelsesGrunnlagPerPerson.hentPersonerMedAvslagIPeriode().filter { it.type == PersonType.BARN }

    val barnMedOppfylteVilkår = hentBarnMedOppfylteVilkår(begrunnelsesGrunnlagPerPerson)
    val barnMedUtbetalingIForrigeperiode =
        hentPersonerMedAndelIForrigePeriode(begrunnelsesGrunnlagPerPerson).filter { it.type == PersonType.BARN }

    return when {
        this.erAvslagUregistrerteBarnBegrunnelse() -> uregistrerteBarnPåBehandlingen.mapNotNull { it.fødselsdato }

        gjelderSøker && !this.gjelderEtterEndretUtbetaling && !this.gjelderEndretutbetaling -> {
            when (this.periodeResultat) {
                SanityPeriodeResultat.IKKE_INNVILGET ->
                    (barnMedUtbetalingIForrigeperiode + barnMedOppfylteVilkår + barnMedEksplisitteAvslag).toSet()
                        .map { it.fødselsdato } +
                        uregistrerteBarnPåBehandlingen.mapNotNull { it.fødselsdato }

                else -> (barnMedUtbetaling + barnPåBegrunnelse).toSet().map { it.fødselsdato }
            }
        }

        else -> {
            barnPåBegrunnelse.map { it.fødselsdato }
        }
    }
}

private fun hentBarnMedOppfylteVilkår(begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelsesGrunnlagPerPerson.filterKeys { it.type == PersonType.BARN }
        .filter { it.value.dennePerioden.vilkårResultater.erOppfyltForBarn() }
        .map { it.key }

fun hentAntallBarnForBegrunnelse(
    begrunnelse: IVedtakBegrunnelse,
    grunnlag: GrunnlagForBegrunnelse,
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
): Int {
    val uregistrerteBarnPåBehandlingen = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn
    val erAvslagUregistrerteBarn = begrunnelse.erAvslagUregistrerteBarnBegrunnelse()

    return when {
        erAvslagUregistrerteBarn -> uregistrerteBarnPåBehandlingen.size
        gjelderSøker && begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG -> 0
        else -> barnasFødselsdatoer.size
    }
}

fun IVedtakBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this in setOf(Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN, EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN)

fun VedtaksperiodeMedBegrunnelser.hentMånedOgÅrForBegrunnelse(): String? {
    return if (this.fom == null || fom == TIDENES_MORGEN) {
        null
    } else {
        fom.forrigeMåned().tilMånedÅr()
    }
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

    else -> emptyList()
}

private fun ISanityBegrunnelse.validerBrevbegrunnelse(
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>,
) {
    if (!gjelderSøker && barnasFødselsdatoer.isEmpty() && !this.gjelderSatsendring && !this.erAvslagUregistrerteBarnBegrunnelse()) {
        throw IllegalStateException("Ingen personer på brevbegrunnelse ${this.apiNavn}")
    }
}

private fun hentSøkersRettTilUtvidet(utvidetUtbetalingsdetaljer: List<AndelForVedtaksperiode>): SøkersRettTilUtvidet {
    return when {
        utvidetUtbetalingsdetaljer.any { it.prosent > BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_FÅR_UTVIDET
        utvidetUtbetalingsdetaljer.isNotEmpty() && utvidetUtbetalingsdetaljer.all { it.prosent == BigDecimal.ZERO } -> SøkersRettTilUtvidet.SØKER_HAR_RETT_MEN_FÅR_IKKE

        else -> SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT
    }
}

enum class SøkersRettTilUtvidet {
    SØKER_FÅR_UTVIDET, SØKER_HAR_RETT_MEN_FÅR_IKKE, SØKER_HAR_IKKE_RETT, ;

    fun tilSanityFormat() = when (this) {
        SØKER_FÅR_UTVIDET -> "sokerFaarUtvidet"
        SØKER_HAR_RETT_MEN_FÅR_IKKE -> "sokerHarRettMenFaarIkke"
        SØKER_HAR_IKKE_RETT -> "sokerHarIkkeRett"
    }
}

fun ISanityBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this.apiNavn in setOf(
        Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN.sanityApiNavn,
        EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN.sanityApiNavn,
    )
