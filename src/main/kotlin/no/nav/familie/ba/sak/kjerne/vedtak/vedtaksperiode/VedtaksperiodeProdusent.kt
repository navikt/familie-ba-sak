package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerBeløpOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDateEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate

private data class VilkårResultatForVedtaksperiode(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val vurderesEtter: Regelverk?,
    val fom: LocalDate?,
    val tom: LocalDate?
)

data class EndretUtbetalingAndelForVedtaksperiode(
    val prosent: BigDecimal,
    val årsak: Årsak,
    val standardbegrunnelse: List<IVedtakBegrunnelse>
)

data class AndelForVedtaksperiode(
    val kalkulertUtbetalingsbeløp: Int,
    val type: YtelseType
)

data class KompetanseForVedtaksperiode(
    val søkersAktivitet: SøkersAktivitet,
    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String,
    val søkersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat
)

private sealed interface GrunnlagForPerson {
    val person: Person
    val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksperiode>
}

private data class GrunnlagForPersonInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode? = null,
    val andeler: Iterable<AndelForVedtaksperiode>? // kan være null for søker
) : GrunnlagForPerson

private data class GrunnlagForPersonIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksperiode>
) : GrunnlagForPerson

/*
hent personResultat
lag ja/nei-tidslinje for søker
for hver person, lag tidslinjer og slå sammen hvis like etterfølgende perioder for
    * vilkårsvurdering
    * endringsperiode
    * kompetanse
    * kalkulert beløp (bruk andel tilkjent ytelse og ta med beløpstype)
        * differanseberegning, ønsker ikke splitt hvis ikke forskjell i beløp
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
fun utledVedtaksPerioderMedBegrunnelser(
    persongrunnlag: PersonopplysningGrunnlag,
    personResultater: Set<PersonResultat>,
    vedtak: Vedtak,
    kompetanser: List<Kompetanse>,
    endredeUtbetalinger: List<EndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
): List<VedtaksperiodeMedBegrunnelser> {
    val søker = persongrunnlag.søker
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val erObligatoriskeVilkårOppfyltForSøkerTidslinje = søkerPersonResultater.tilTidslinjeForSplittForPerson(
        personType = søker.type,
        fagsakType = vedtak.behandling.fagsak.type
    ).map { it != null }

    val utfylteEndredeUtbetalinger = endredeUtbetalinger
        .map { it.tilIEndretUtbetalingAndel() }
        .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    val utfylteKompetanser = kompetanser
        .map { it.tilIKompetanse() }
        .filterIsInstance<UtfyltKompetanse>()

    val erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje =
        hentErObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje(personResultater, søker, vedtak)

    val grunnlagForPersonTidslinjer = personResultater
        .map { personResultat ->
            personResultat.tilGrunnlagForPersonTidslinje(
                persongrunnlag = persongrunnlag,
                erObligatoriskeVilkårOppfyltForSøkerTidslinje = erObligatoriskeVilkårOppfyltForSøkerTidslinje,
                erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje = erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje,
                vedtak = vedtak,
                kompetanser = utfylteKompetanser.filter { kompetanse -> kompetanse.barnAktører.contains(personResultat.aktør) },
                endredeUtbetalinger = utfylteEndredeUtbetalinger
                    .filter { endretUtbetaling -> endretUtbetaling.person.aktør == personResultat.aktør },
                andelerTilkjentYtelse = andelerTilkjentYtelse.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == personResultat.aktør }
            )
        }

    val perioderSomSkalBegrunnesIVedtak = grunnlagForPersonTidslinjer.tilPerioderSomSkalBegrunnesIVedtak()

    return perioderSomSkalBegrunnesIVedtak.map {
        it.tilVedtaksperiodeMedBegrunnelser(vedtak)
    }
}

// TODO: hva gjør vi hvis søker er et barn?
private fun hentErObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje(
    personResultater: Set<PersonResultat>,
    søker: Person,
    vedtak: Vedtak
): Tidslinje<Boolean, Måned> = personResultater
    .filter { it.aktør != søker.aktør }
    .map { personResultat ->
        personResultat.tilTidslinjeForSplittForPerson(
            personType = PersonType.BARN,
            fagsakType = vedtak.behandling.fagsak.type
        ).map { it != null }
    }.kombiner { it.any() }

private fun Periode<List<GrunnlagForPerson?>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
    tom = tilOgMed.tilLocalDateEllerNull(),
    type = if (innhold?.any { it is GrunnlagForPersonInnvilget } == true) {
        Vedtaksperiodetype.UTBETALING
    } else {
        Vedtaksperiodetype.OPPHØR
    }
)

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
private fun List<Tidslinje<GrunnlagForPerson, Måned>>.tilPerioderSomSkalBegrunnesIVedtak(): List<Periode<List<GrunnlagForPerson?>, Måned>> {
    val sammenslåtteInnvilgedePerioder = map { it.filtrerHarInnholdstype<GrunnlagForPersonInnvilget>() }
        .kombiner { it }
        .perioder()

    val ikkeInnvilgedePerioder: List<Periode<List<GrunnlagForPersonIkkeInnvilget?>, Måned>> =
        map { it.filtrerHarInnholdstype<GrunnlagForPersonIkkeInnvilget>() }
            .map { grunnlagForPersonTidslinje -> grunnlagForPersonTidslinje.map { listOf(it) } }
            .flatMap { it.perioder() }

    return (ikkeInnvilgedePerioder + sammenslåtteInnvilgedePerioder).slåSammenPerioderMedSammeFomOgTom()
}

private fun List<Periode<out Iterable<GrunnlagForPerson?>, Måned>>.slåSammenPerioderMedSammeFomOgTom() =
    this.groupBy { Pair(it.fraOgMed, it.tilOgMed) }
        .map {
            Periode(
                fraOgMed = it.key.first,
                tilOgMed = it.key.second,
                innhold = it.value.flatMap { periode -> periode.innhold ?: emptyList() }
            )
        }

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : GrunnlagForPerson> Tidslinje<GrunnlagForPerson, Måned>.filtrerHarInnholdstype(): Tidslinje<T, Måned> =
    this.perioder()
        .filter { it.innhold is T }
        .tilTidslinje() as Tidslinje<T, Måned>

private fun PersonResultat.tilGrunnlagForPersonTidslinje(
    persongrunnlag: PersonopplysningGrunnlag,
    erObligatoriskeVilkårOppfyltForSøkerTidslinje: Tidslinje<Boolean, Måned>,
    erObligatoriskeVilkårOppfyltForMinstEttBarnTidslinje: Tidslinje<Boolean, Måned>,
    vedtak: Vedtak,
    kompetanser: List<UtfyltKompetanse>,
    endredeUtbetalinger: List<IUtfyltEndretUtbetalingAndel>,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
): Tidslinje<GrunnlagForPerson, Måned> {
    val person = persongrunnlag.personer.single { person -> this.aktør == person.aktør }

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
            fagsakType = vedtak.behandling.fagsak.type,
            personType = person.type
        )

    val vilkårResultaterTidslinje = forskjøvedeVilkårResultaterForPerson.tilVilkårResultaterForVedtaksPeriodeTidslinje()
    val kompetanseTidslinje = kompetanser.tilTidslinje()
        .mapIkkeNull { it.tilKompetanseForVedtaksPeriode() }

    val endredeUtbetalingerTidslinje = endredeUtbetalinger.tilTidslinje()
        .mapIkkeNull { it.tilEndretUtbetalingAndelForVedtaksPeriode() }

    val andelerTidslinje =
        andelerTilkjentYtelse.tilTidslinjerPerBeløpOgType()
            .values
            .map { tidslinje -> tidslinje.mapIkkeNull { it.tilAndelForVedtaksperiode() } }
            .kombiner { it }

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
        }.filtrerIkkeNull()

    return grunnlagTidslinje.slåSammenLike()
}

private fun AndelTilkjentYtelse.tilAndelForVedtaksperiode() =
    AndelForVedtaksperiode(
        kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
        type = type
    )

private fun IUtfyltEndretUtbetalingAndel.tilEndretUtbetalingAndelForVedtaksPeriode() =
    EndretUtbetalingAndelForVedtaksperiode(
        prosent = prosent,
        årsak = årsak,
        standardbegrunnelse = standardbegrunnelser
    )

private fun UtfyltKompetanse.tilKompetanseForVedtaksPeriode() =
    KompetanseForVedtaksperiode(
        søkersAktivitet = søkersAktivitet,
        annenForeldersAktivitet = annenForeldersAktivitet,
        annenForeldersAktivitetsland = annenForeldersAktivitetsland,
        søkersAktivitetsland = søkersAktivitetsland,
        barnetsBostedsland = barnetsBostedsland,
        resultat = resultat
    )

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

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    map { vilkårResultater ->
        vilkårResultater?.map {
            VilkårResultatForVedtaksperiode(
                vilkårType = it.vilkårType,
                resultat = it.resultat,
                utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger,
                vurderesEtter = it.vurderesEtter,
                fom = it.periodeFom,
                tom = it.periodeTom
            )
        }
    }

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilErVilkårsvurderingOppfyltTidslinje(
    erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje: Tidslinje<Boolean, Måned>,
    fagsakType: FagsakType,
    personType: PersonType
) =
    kombinerMed(erObligatoriskeVilkårOppfyltForAnnenRelevantPersonTidslinje) { oppfylteVilkårNullable, erObligatoriskeVilkårOppfyltForAnnenRelevantPerson ->
        val oppfylteVilkår = (oppfylteVilkårNullable ?: emptyList())

        erObligatoriskeVilkårOppfyltForAnnenRelevantPerson ?: false && oppfylteVilkår.alleOrdinæreVilkårErOppfylt(
            personType = personType,
            fagsakType = fagsakType
        )
    }
