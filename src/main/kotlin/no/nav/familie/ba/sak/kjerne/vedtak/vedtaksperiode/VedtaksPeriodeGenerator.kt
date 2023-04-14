package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

private data class VilkårResultatForVedtaksPeriode(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val vurderesEtter: Regelverk?
)

private sealed interface GrunnlagForPerson {
    val person: Person
    val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksPeriode>
}

private data class GrunnlagForPersonInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksPeriode>
    // endringsperiode
    // kompentanse,
    // beløp
    // diffberegning?
) : GrunnlagForPerson

private data class GrunnlagForPersonIkkeInnvilget(
    override val person: Person,
    override val vilkårResultaterForVedtaksPeriode: List<VilkårResultatForVedtaksPeriode>
) : GrunnlagForPerson

/*
hent personResultat
lag ja/nei-tidslinje for søker
for hver person, lag tidslinjer og slå sammen hvis like etterfølgende perioder for
    * vilkårsvurdering
    * endringsperiode
    * kompetanse
    * kalkulert beløp (bruk andel tilkjent ytelse og ta med beløpstype)
    * differansebereginig?
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
    vedtak: Vedtak
): List<VedtaksperiodeMedBegrunnelser> {
    val søker = persongrunnlag.søker
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val erObligatoriskeVilkårOppfyltForSøkerTidslinje = søkerPersonResultater.tilTidslinjeForSplittForPerson(
        personType = søker.type,
        fagsakType = vedtak.behandling.fagsak.type
    ).map { it != null }

    val grunnlagForPersonTidslinjer = personResultater.map {
        it.tilGrunnlagForPersonTidslinje(
            persongrunnlag = persongrunnlag,
            erObligatoriskeVilkårOppfyltForSøkerTidslinje = erObligatoriskeVilkårOppfyltForSøkerTidslinje,
            vedtak = vedtak
        )
    }

    val perioderSomSkalBegrunnesIVedtak = grunnlagForPersonTidslinjer.tilPerioderSomSkalBegrunnesIVedtak()

    return perioderSomSkalBegrunnesIVedtak.map {
        it.tilVedtaksperiodeMedBegrunnelser(vedtak)
    }
}

private fun Periode<List<GrunnlagForPerson?>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fraOgMed.tilLocalDate(),
    tom = tilOgMed.tilLocalDate(),
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
 * I eksempelet kan man se at alle innvilgede perioder blir slått sammen. I tillegg blir også de ikke-innvilgede 
 * periodene med samme fom og tom slått sammen med de innvilgede periodene, men de resterende ikke-innvilgede blir 
 * stående for seg.
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
                innhold = it.value.flatMap { periode -> periode.innhold ?: emptyList() })
        }

private fun <T : GrunnlagForPerson> Tidslinje<GrunnlagForPerson, Måned>.filtrerHarInnholdstype(): Tidslinje<T, Måned> =
    this.perioder()
        .filterIsInstance<Periode<T, Måned>>()
        .tilTidslinje()

private fun PersonResultat.tilGrunnlagForPersonTidslinje(
    persongrunnlag: PersonopplysningGrunnlag,
    erObligatoriskeVilkårOppfyltForSøkerTidslinje: Tidslinje<Boolean, Måned>,
    vedtak: Vedtak
): Tidslinje<GrunnlagForPerson, Måned> {
    val person = persongrunnlag.personer.single { aktør == it.aktør }

    val forskjøvedeVilkårResultater =
        vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår().kombiner { it }

    val erVilkårsvurderingOppfyltTidslinje: Tidslinje<Boolean, Måned> = forskjøvedeVilkårResultater
        .tilErVilkårsvurderingOppfyltTidslinje(
            erObligatoriskeVilkårOppfyltForSøkerTidslinje = erObligatoriskeVilkårOppfyltForSøkerTidslinje,
            fagsakType = vedtak.behandling.fagsak.type,
            personType = person.type,
        )

    val vilkårResultaterTidslinje = forskjøvedeVilkårResultater.tilVilkårResultaterForVedtaksPeriodeTidslinje()

    val grunnlagTidslinje = erVilkårsvurderingOppfyltTidslinje
        .kombinerMed(vilkårResultaterTidslinje) { erVilkårsvurderingOppfylt, vilkårResultater ->
            lagGrunnlagForVilkår(erVilkårsvurderingOppfylt, vilkårResultater, person)
        }

    return grunnlagTidslinje.slåSammenLike()
}

private fun lagGrunnlagForVilkår(
    erVilkårsvurderingOppfylt: Boolean?,
    vilkårResultater: List<VilkårResultatForVedtaksPeriode>?,
    person: Person
) = if (erVilkårsvurderingOppfylt == true) {
    GrunnlagForPersonInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater
            ?: error("vilkårResultatene burde alltid finnes om vi har rett"),
        person = person
    )
} else {
    GrunnlagForPersonIkkeInnvilget(
        vilkårResultaterForVedtaksPeriode = vilkårResultater ?: emptyList(),
        person = person
    )
}

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    map { vilkårResultater ->
        vilkårResultater?.map {
            VilkårResultatForVedtaksPeriode(
                it.vilkårType,
                it.resultat,
                it.utdypendeVilkårsvurderinger,
                it.vurderesEtter
            )
        }
    }

private fun Tidslinje<Iterable<VilkårResultat>, Måned>.tilErVilkårsvurderingOppfyltTidslinje(
    erObligatoriskeVilkårOppfyltForSøkerTidslinje: Tidslinje<Boolean, Måned>,
    fagsakType: FagsakType,
    personType: PersonType,
) =
    kombinerMed(erObligatoriskeVilkårOppfyltForSøkerTidslinje) { oppfylteVilkårNullable, erObligatoriskeVilkårOppfyltForSøker ->
        val oppfylteVilkår = (oppfylteVilkårNullable ?: emptyList())

        erObligatoriskeVilkårOppfyltForSøker ?: false && oppfylteVilkår.alleOrdinæreVilkårErOppfylt(
            personType = personType,
            fagsakType = fagsakType
        )
    }
