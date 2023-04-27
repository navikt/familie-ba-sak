package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerBeløpOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDateEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

/**
 * Vi ønsker å ha en kombinert tidslinje med alle innvilgede perioder og ikke-innvilgede som sammenfaller på dato.
 *
 * I tillegg vil vi ha ikke-innvilgede perioder som ikke sammenfaller på dato som egne tidslinjer
 *
 * Vi ønsker ikke å splitte opp perioder som ikke er innvilgede, men vi ønsker å slå dem sammen med innvilgede perioder
 * med samme fom og tom.
 *
 * Se src/test/resources/kjerne/vedtak.vedtaksperiode/VilkårPerBarnBlirSlåttSammenTilPerioderSomSkalBegrunnesIVedtak.png
 *
 * I eksempelet kan man se at alle innvilgede perioder blir slått sammen. I tillegg blir også de ikke-innvilgede * periodene med samme fom og tom slått sammen med de innvilgede periodene, men de resterende ikke-innvilgede blir * stående for seg.
 **/
fun finnPerioderSomSkalBegrunnes(
    grunnlagTidslinjePerPerson: Map<Person, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<GrunnlagForPerson, Måned>>
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling?>, Måned>> {
    val gjeldendeOgForrigeGrunnlagKombinert = kombinerGjeldendeOgForrigeGrunnlag(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling
    )

    val sammenslåtteInnvilgedePerioder = gjeldendeOgForrigeGrunnlagKombinert.utledSammenslåttePerioder()
    val ikkeInnvilgedePerioder = gjeldendeOgForrigeGrunnlagKombinert.utledIkkeinnvilgedePerioder()

    return (ikkeInnvilgedePerioder + sammenslåtteInnvilgedePerioder).slåSammenPerioderMedSammeFomOgTom()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledSammenslåttePerioder() = this
    .map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer { it?.gjeldende is GrunnlagForPersonInnvilget }
    }.kombiner { it }
    .perioder()

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledIkkeinnvilgedePerioder() = this
    .map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer {
            val gjeldendeErIkkeInnvilget = it?.gjeldende is GrunnlagForPersonIkkeInnvilget
            val gjeldendeErNullForrigeErInnvilget = it?.gjeldende == null && it?.forrige is GrunnlagForPersonInnvilget

            gjeldendeErIkkeInnvilget || gjeldendeErNullForrigeErInnvilget
        }
    }.map { grunnlagForPersonTidslinje -> grunnlagForPersonTidslinje.map { listOf(it) } }
    .flatMap { it.perioder() }

/**
 * Ønsker å dra med informasjon om forrige behandling i perioder der forrige behandling var oppfylt, men gjeldende
 * ikke er det.
 **/
private fun kombinerGjeldendeOgForrigeGrunnlag(
    grunnlagTidslinjePerPerson: Map<Person, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<GrunnlagForPerson, Måned>>
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    grunnlagTidslinjePerPerson.map { (person, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[person]

        grunnlagstidslinje.kombinerMed(grunnlagForrigeBehandling ?: TomTidslinje()) { gjeldende, forrige ->
            val gjeldendeErIkkeOppfylt = gjeldende !is GrunnlagForPersonInnvilget
            val forrigeErOppfylt = forrige is GrunnlagForPersonInnvilget

            if (gjeldendeErIkkeOppfylt && forrigeErOppfylt) {
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, forrige)
            } else {
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, null)
            }
        }.slåSammenLike()
    }

fun genererVedtaksperioder(
    grunnlagForVedtakPerioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtakPerioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    vedtak: Vedtak
): List<VedtaksperiodeMedBegrunnelser> {
    val grunnlagTidslinjePerPerson = utledGrunnlagTidslinjePerPerson(grunnlagForVedtakPerioder)

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtakPerioderForrigeBehandling
            ?.let { utledGrunnlagTidslinjePerPerson(grunnlagForVedtakPerioderForrigeBehandling) }
            ?: emptyMap()

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(grunnlagTidslinjePerPerson, grunnlagTidslinjePerPersonForrigeBehandling)

    return perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak) }
}

/*
hent personResultat
lag ja/nei-tidslinje for søker
for hver person, lag tidslinjer og slå sammen hvis like etterfølgende perioder for
    * vilkårsvurdering
    * endringsperiode
    * kompetanse
    * kalkulert beløp (bruk andel tilkjent ytelse og ta med beløpstype)
        * overgangsstønad
        * satsendring
        * endring i alder
    * ja/nei (avslag/opphørt)
    Trenger vi endringsperiode, kompetanse og kalkulert beløp i nei-perioder?

kombiner ja og andre kontekster til en tidslinje
behold nei for seg

kombiner peroide uten rett med ja hvis fom og tom er like på tvers av personer
kombiner nei hvis fom og tom er like på tvers av personer
behold enkeltstående nei
*/
private fun utledGrunnlagTidslinjePerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder
): Map<Person, Tidslinje<GrunnlagForPerson, Måned>> {
    val (persongrunnlag, personResultater, fagsakType, kompetanser, endredeUtbetalinger, andelerTilkjentYtelse, perioderOvergangsstønad) = grunnlagForVedtaksperioder

    val søker = persongrunnlag.søker
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val erObligatoriskeVilkårOppfyltForSøkerTidslinje = søkerPersonResultater.tilTidslinjeForSplittForPerson(
        personType = søker.type,
        fagsakType = fagsakType
    ).map { it != null }

    val utfylteEndredeUtbetalinger = endredeUtbetalinger
        .map { it.tilIEndretUtbetalingAndel() }
        .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    val utfylteKompetanser = kompetanser
        .map { it.tilIKompetanse() }
        .filterIsInstance<UtfyltKompetanse>()

    val erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje =
        hentErObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje(personResultater, søker, fagsakType)

    val grunnlagForPersonTidslinjer = personResultater
        .associate { personResultat ->
            val person = persongrunnlag.personer.single { person -> personResultat.aktør == person.aktør }

            person to personResultat.tilGrunnlagForPersonTidslinje(
                person = person,
                erObligatoriskeVilkårOppfyltForSøkerTidslinje = erObligatoriskeVilkårOppfyltForSøkerTidslinje,
                erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje = erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje,
                fagsakType = fagsakType,
                kompetanser = utfylteKompetanser.filter { kompetanse -> kompetanse.barnAktører.contains(personResultat.aktør) },
                endredeUtbetalinger = utfylteEndredeUtbetalinger
                    .filter { endretUtbetaling -> endretUtbetaling.person.aktør == personResultat.aktør },
                andelerTilkjentYtelse = andelerTilkjentYtelse.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == personResultat.aktør },
                perioderOvergangsstønad = perioderOvergangsstønad.filter { it.aktør == person.aktør }
            ).perioder()
                .dropWhile { it.innhold !is GrunnlagForPersonInnvilget }
                .tilTidslinje()
        }

    return grunnlagForPersonTidslinjer
}

// TODO: hva gjør vi hvis søker er et barn?
private fun hentErObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje(
    personResultater: Set<PersonResultat>,
    søker: Person,
    fagsakType: FagsakType
): Tidslinje<Boolean, Måned> = personResultater
    .filter { it.aktør != søker.aktør }
    .map { personResultat ->
        personResultat.tilTidslinjeForSplittForPerson(
            personType = PersonType.BARN,
            fagsakType = fagsakType
        ).map { it != null }
    }.kombiner { it.any() }

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling?>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
    tom = tilOgMed.tilLocalDateEllerNull(),
    type = if (innhold?.any { it?.gjeldende is GrunnlagForPersonInnvilget } == true) {
        Vedtaksperiodetype.UTBETALING
    } else {
        Vedtaksperiodetype.OPPHØR
    }
)

private fun <T> List<Periode<out Iterable<T>, Måned>>.slåSammenPerioderMedSammeFomOgTom() =
    this.groupBy { Pair(it.fraOgMed, it.tilOgMed) }
        .map {
            Periode(
                fraOgMed = it.key.first,
                tilOgMed = it.key.second,
                innhold = it.value.mapNotNull { periode -> periode.innhold }.flatten()
            )
        }

private fun PersonResultat.tilGrunnlagForPersonTidslinje(
    person: Person,
    erObligatoriskeVilkårOppfyltForSøkerTidslinje: Tidslinje<Boolean, Måned>,
    erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje: Tidslinje<Boolean, Måned>,
    fagsakType: FagsakType,
    kompetanser: List<UtfyltKompetanse>,
    endredeUtbetalinger: List<IUtfyltEndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    perioderOvergangsstønad: List<PeriodeOvergangsstønadGrunnlag>
): Tidslinje<GrunnlagForPerson, Måned> {
    val forskjøvedeVilkårResultater = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår().kombiner { it }
    val forskjøvedeVilkårResultaterForPerson = when (person.type) {
        PersonType.BARN -> forskjøvedeVilkårResultater
        PersonType.SØKER ->
            forskjøvedeVilkårResultater
                .beskjærEtter(erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje)

        PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
    }

    @Suppress("KotlinConstantConditions")
    val erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje = when (person.type) {
        PersonType.SØKER -> erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje
        PersonType.BARN ->
            erObligatoriskeVilkårOppfyltForSøkerTidslinje
                .beskjærEtter(forskjøvedeVilkårResultaterForPerson)

        PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
    }

    val erVilkårsvurderingOppfyltTidslinje = forskjøvedeVilkårResultaterForPerson
        .tilErVilkårsvurderingOppfyltTidslinje(
            erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje = erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje,
            fagsakType = fagsakType,
            personType = person.type
        )

    val vilkårResultaterTidslinje = forskjøvedeVilkårResultaterForPerson.tilVilkårResultaterForVedtaksPeriodeTidslinje()

    val kompetanseTidslinje = kompetanser.tilTidslinje()
        .mapIkkeNull { KompetanseForVedtaksperiode(it) }

    val endredeUtbetalingerTidslinje = endredeUtbetalinger.tilTidslinje()
        .mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

    val andelerTidslinje =
        andelerTilkjentYtelse.tilTidslinjerPerBeløpOgType()
            .values
            .map { tidslinje -> tidslinje.mapIkkeNull { AndelForVedtaksperiode(it) } }
            .kombiner { it }

    val overgangsstønadTidslinje = perioderOvergangsstønad.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje()

    val grunnlagTidslinje = erVilkårsvurderingOppfyltTidslinje
        .kombinerMed(
            vilkårResultaterTidslinje,
            andelerTidslinje
        ) { erVilkårsvurderingOppfylt, vilkårResultater, andeler ->
            lagGrunnlagForVilkårOgAndel(
                erVilkårsvurderingOppfylt = erVilkårsvurderingOppfylt,
                vilkårResultater = vilkårResultater,
                person = person,
                andeler = andeler
            )
        }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
            lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
        }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
            lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
        }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
            lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
        }.filtrerIkkeNull()

    return grunnlagTidslinje.slåSammenLike()
}

private fun List<PeriodeOvergangsstønadGrunnlag>.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje() = this
    .map { OvergangsstønadForVedtaksperiode(it) }
    .map { Periode(it.fom.tilMånedTidspunkt(), it.tom.tilMånedTidspunkt(), it) }
    .tilTidslinje()

private fun lagGrunnlagForVilkårOgAndel(
    erVilkårsvurderingOppfylt: Boolean?,
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>?
) = if (erVilkårsvurderingOppfylt == true) {
    GrunnlagForPersonInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater
            ?: error("vilkårResultatene burde alltid finnes om vi har rett"),
        person = person,
        andeler = andeler
    )
} else {
    GrunnlagForPersonIkkeInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater ?: emptyList(),
        person = person
    )
}

private fun lagGrunnlagMedKompetanse(
    grunnlagForPerson: GrunnlagForPerson?,
    kompetanse: KompetanseForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(kompetanse = kompetanse)
    is GrunnlagForPersonIkkeInnvilget -> {
        if (kompetanse != null) {
            throw Feil("GrunnlagForPersonIkkeInnvilget for aktør ${grunnlagForPerson.person.aktør} kan ikke ha kompetanse siden den ikke er innvilget")
        }
        grunnlagForPerson
    }

    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    grunnlagForPerson: GrunnlagForPerson?,
    endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is GrunnlagForPersonIkkeInnvilget -> {
        if (endretUtbetalingAndel != null) {
            throw Feil("GrunnlagForPersonIkkeInnvilget for aktør ${grunnlagForPerson.person.aktør} kan ikke ha endretUtbetalingAndel siden den ikke er innvilget")
        }
        grunnlagForPerson
    }

    null -> null
}

private fun lagGrunnlagMedOvergangsstønad(
    grunnlagForPerson: GrunnlagForPerson?,
    overgangsstønad: OvergangsstønadForVedtaksperiode?
) = when (grunnlagForPerson) {
    is GrunnlagForPersonInnvilget -> grunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is GrunnlagForPersonIkkeInnvilget -> grunnlagForPerson
    null -> null
}

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    this.map { vilkårResultater -> vilkårResultater?.map { VilkårResultatForVedtaksperiode(it) } }

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilErVilkårsvurderingOppfyltTidslinje(
    erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje: Tidslinje<Boolean, Måned>,
    fagsakType: FagsakType,
    personType: PersonType
) = this.kombinerMed(
    erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje
) { oppfylteVilkårNullable, erObligatoriskeVilkårOppfyltForAnnenRelevantPerson ->
    val oppfylteVilkår = (oppfylteVilkårNullable ?: emptyList())

    erObligatoriskeVilkårOppfyltForAnnenRelevantPerson ?: false && oppfylteVilkår.alleOrdinæreVilkårErOppfylt(
        personType = personType,
        fagsakType = fagsakType
    )
}
