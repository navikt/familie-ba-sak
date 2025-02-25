package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelForVedtaksbegrunnelseTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.IEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.KompetanseForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.OvergangsstønadForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.UtenlandskPeriodebeløpForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.ValutakursForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.filtrerPåAktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.hentErUtbetalingSmåbarnstilleggTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.Companion.hentOrdinæreVilkårFor
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import java.math.BigDecimal

data class BegrunnelseGrunnlagForPersonIPeriode(
    val person: Person,
    val vilkårResultater: Iterable<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksbegrunnelse>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val utenlandskPeriodebeløp: UtenlandskPeriodebeløpForVedtaksperiode? = null,
    val valutakurs: ValutakursForVedtaksperiode? = null,
    val endretUtbetalingAndel: IEndretUtbetalingAndelForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
    val eksplisitteAvslagForPerson: List<VilkårResultatForVedtaksperiode>? = null,
) {
    fun erOrdinæreVilkårInnvilget() =
        hentOrdinæreVilkårFor(person.type).all { ordinærtVilkårForPerson ->
            vilkårResultater.any { it.vilkårType == ordinærtVilkårForPerson && it.resultat == Resultat.OPPFYLT }
        }

    fun erInnvilgetEtterEndretUtbetaling(): Boolean {
        val erEndretUtbetaling = endretUtbetalingAndel != null
        val erEndretUtbetalingPåNullProsent = endretUtbetalingAndel?.prosent == BigDecimal.ZERO
        val erÅrsakDeltBosted = endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED

        return !erEndretUtbetaling || !erEndretUtbetalingPåNullProsent || erÅrsakDeltBosted
    }

    companion object {
        fun tomPeriode(person: Person) = BegrunnelseGrunnlagForPersonIPeriode(person = person, vilkårResultater = emptyList(), andeler = emptyList())
    }
}

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer(): Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode>> = this.persongrunnlag.personer.associateWith { this.lagBegrunnelseGrunnlagForPersonTidslinje(it) }

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagForPersonTidslinje(
    person: Person,
): Tidslinje<BegrunnelseGrunnlagForPersonIPeriode> {
    val vilkårResultaterForPerson =
        this.personResultater.singleOrNull { it.aktør == person.aktør }?.vilkårResultater ?: emptyList()

    val (generelleAvslag, vilkårResultaterMedPerioder) = vilkårResultaterForPerson.partition { it.erEksplisittAvslagUtenPeriode() }

    val forskjøvedeVilkårMedPeriode = vilkårResultaterMedPerioder.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).map { tidslinje -> tidslinje.mapVerdi { it?.let { VilkårResultatForVedtaksperiode(it) } } }

    val forskjøvedeVilkårTidslinje = forskjøvedeVilkårMedPeriode.map { tidslinje -> tidslinje.filtrer { it?.erEksplisittAvslagPåSøknad != true } }.kombiner { it }

    val eksplisitteAvslagTidslinje =
        lagTidslinjeForEksplisitteAvslag(forskjøvedeVilkårMedPeriode, generelleAvslag)

    val kompetanseTidslinje =
        this.utfylteKompetanser
            .filtrerPåAktør(person.aktør)
            .tilTidslinje()
            .mapIkkeNull { KompetanseForVedtaksperiode(it) }

    val utenlandskPeriodebeløpTidslinje =
        utfylteUtenlandskPeriodebeløp
            .filtrerPåAktør(person.aktør)
            .tilTidslinje()
            .mapIkkeNull { UtenlandskPeriodebeløpForVedtaksperiode(it) }

    val valutakursTidslinje =
        utfylteValutakurs
            .filtrerPåAktør(person.aktør)
            .tilTidslinje()
            .mapIkkeNull { ValutakursForVedtaksperiode(it) }

    val endredeUtbetalingerTidslinje =
        this.utfylteEndredeUtbetalinger
            .filtrerPåAktør(person.aktør)
            .tilTidslinje()
            .mapIkkeNull { it.tilEndretUtbetalingAndelForVedtaksperiode() }

    val andelerTilkjentYtelseTidslinje =
        this.andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksbegrunnelseTidslinje()

    val overgangsstønadTidslinje =
        this.perioderOvergangsstønad
            .filtrerPåAktør(person.aktør)
            .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(andelerTilkjentYtelseTidslinje.hentErUtbetalingSmåbarnstilleggTidslinje())

    return forskjøvedeVilkårTidslinje
        .kombinerMed(
            andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksbegrunnelseTidslinje(),
        ) { vilkårResultater, andeler ->
            vilkårResultater?.let {
                BegrunnelseGrunnlagForPersonIPeriode(
                    person = person,
                    vilkårResultater = vilkårResultater,
                    andeler = andeler ?: emptyList(),
                )
            }
        }.kombinerMed(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
            grunnlagForPerson?.copy(kompetanse = kompetanse)
        }.kombinerMed(valutakursTidslinje) { grunnlagForPerson, valutakurs ->
            grunnlagForPerson?.copy(valutakurs = valutakurs)
        }.kombinerMed(utenlandskPeriodebeløpTidslinje) { grunnlagForPerson, utenlandskPeriodebeløp ->
            grunnlagForPerson?.copy(utenlandskPeriodebeløp = utenlandskPeriodebeløp)
        }.kombinerMed(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
            grunnlagForPerson?.copy(endretUtbetalingAndel = endretUtbetalingAndel)
        }.kombinerMed(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
            grunnlagForPerson?.copy(overgangsstønad = overgangsstønad)
        }.kombinerMed(eksplisitteAvslagTidslinje) { grunnlagForPerson, eksplisitteAvslag ->
            if (eksplisitteAvslag.isNullOrEmpty()) {
                grunnlagForPerson
            } else {
                grunnlagForPerson?.copy(eksplisitteAvslagForPerson = eksplisitteAvslag)
                    ?: BegrunnelseGrunnlagForPersonIPeriode(
                        person = person,
                        vilkårResultater = emptyList(),
                        andeler = emptyList(),
                        eksplisitteAvslagForPerson = eksplisitteAvslag,
                    )
            }
        }
}

private fun lagTidslinjeForEksplisitteAvslag(
    forskjøvedeVilkårMedPeriode: List<Tidslinje<VilkårResultatForVedtaksperiode>>,
    generelleAvslag: List<VilkårResultat>,
): Tidslinje<List<VilkårResultatForVedtaksperiode>> {
    val forskjøvedeEksplisitteAvslagMedPerioder = forskjøvedeVilkårMedPeriode.map { tidslinje -> tidslinje.filtrer { it?.erEksplisittAvslagPåSøknad == true } }.kombiner { it.toList() }
    val eksplisitteAvslagUtenPeriode = generelleAvslag.map { genereltAvslag -> Periode(genereltAvslag, null, null).tilTidslinje().mapVerdi { it?.let { VilkårResultatForVedtaksperiode(it) } } }.kombiner { it.toList() }

    val eksplisitteAvslagTidslinje =
        forskjøvedeEksplisitteAvslagMedPerioder.kombinerMed(eksplisitteAvslagUtenPeriode) { avslagMedPeriode, avslagUtenPeriode ->
            when {
                avslagMedPeriode == null -> avslagUtenPeriode
                avslagUtenPeriode == null -> avslagMedPeriode
                else -> avslagMedPeriode + avslagUtenPeriode
            }
        }
    return eksplisitteAvslagTidslinje
}

fun List<AndelTilkjentYtelse>.tilAndelerForVedtaksbegrunnelseTidslinje(): Tidslinje<Iterable<AndelForVedtaksbegrunnelse>> =
    this
        .tilAndelForVedtaksbegrunnelseTidslinjerPerAktørOgType()
        .values
        .map { tidslinje -> tidslinje.mapIkkeNull { it }.slåSammenLikePerioder() }
        .kombiner()
