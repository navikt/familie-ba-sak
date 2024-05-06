package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.IEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.KompetanseForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.OvergangsstønadForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.UtenlandskPeriodebeløpForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.ValutakursForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.filtrerPåAktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.hentErUtbetalingSmåbarnstilleggTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilAndelerForVedtaksPeriodeTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.Companion.hentOrdinæreVilkårFor
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import java.math.BigDecimal

data class BegrunnelseGrunnlagForPersonIPeriode(
    val person: Person,
    val vilkårResultater: Iterable<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
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
        fun tomPeriode(person: Person) =
            BegrunnelseGrunnlagForPersonIPeriode(person = person, vilkårResultater = emptyList(), andeler = emptyList())
    }
}

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer(): Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>> {
    return this.persongrunnlag.personer.associateWith { this.lagBegrunnelseGrunnlagForPersonTidslinje(it) }
}

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagForPersonTidslinje(
    person: Person,
): Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned> {
    val vilkårResultaterForPerson =
        this.personResultater.singleOrNull { it.aktør == person.aktør }?.vilkårResultater ?: emptyList()

    val forskjøvedeVilkårMedPeriode = vilkårResultaterForPerson.filter { !it.erAvslagUtenPeriode() }.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).map { tidslinje -> tidslinje.map { it?.let { VilkårResultatForVedtaksperiode(it) } } }

    val forskjøvedeVilkår = forskjøvedeVilkårMedPeriode.map { tidslinje -> tidslinje.filtrer { it?.erEksplisittAvslagPåSøknad != true } }.kombiner { it }

    val forskjøvedeEksplisitteAvslagMedPerioder = forskjøvedeVilkårMedPeriode.map { tidslinje -> tidslinje.filtrer { it?.erEksplisittAvslagPåSøknad == true } }.kombiner { it.toList() }
    val eksplisitteAvslagUtenPeriode = vilkårResultaterForPerson.filter { it.erAvslagUtenPeriode() }.tilTidslinje().map { it?.let { VilkårResultatForVedtaksperiode(it) } }.tilMåned { it.first() }

    val eksplisitteAvslagTidslinje =
        forskjøvedeEksplisitteAvslagMedPerioder.kombinerMed(eksplisitteAvslagUtenPeriode) { medPeriode, utenPeriode ->
            when {
                medPeriode == null -> listOfNotNull(utenPeriode)
                utenPeriode == null -> medPeriode
                else -> medPeriode + listOf(utenPeriode)
            }
        }

    val kompetanseTidslinje =
        this.utfylteKompetanser.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

    val utenlandskPeriodebeløpTidslinje =
        utfylteUtenlandskPeriodebeløp.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { UtenlandskPeriodebeløpForVedtaksperiode(it) }

    val valutakursTidslinje =
        utfylteValutakurs.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { ValutakursForVedtaksperiode(it) }

    val endredeUtbetalingerTidslinje =
        this.utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { it.tilEndretUtbetalingAndelForVedtaksperiode() }

    val andelerTilkjentYtelseTidslinje =
        this.andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje()

    val overgangsstønadTidslinje =
        this.perioderOvergangsstønad.filtrerPåAktør(person.aktør)
            .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(andelerTilkjentYtelseTidslinje.hentErUtbetalingSmåbarnstilleggTidslinje())

    return forskjøvedeVilkår
        .kombinerMed(
            andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje(),
        ) { vilkårResultater, andeler ->
            vilkårResultater?.let {
                BegrunnelseGrunnlagForPersonIPeriode(
                    person = person,
                    vilkårResultater = vilkårResultater,
                    andeler = andeler ?: emptyList(),
                )
            }
        }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
            grunnlagForPerson?.copy(kompetanse = kompetanse)
        }.kombinerMedNullable(valutakursTidslinje) { grunnlagForPerson, valutakurs ->
            grunnlagForPerson?.copy(valutakurs = valutakurs)
        }.kombinerMedNullable(utenlandskPeriodebeløpTidslinje) { grunnlagForPerson, utenlandskPeriodebeløp ->
            grunnlagForPerson?.copy(utenlandskPeriodebeløp = utenlandskPeriodebeløp)
        }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
            grunnlagForPerson?.copy(endretUtbetalingAndel = endretUtbetalingAndel)
        }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
            grunnlagForPerson?.copy(overgangsstønad = overgangsstønad)
        }.kombinerMedNullable(eksplisitteAvslagTidslinje) { grunnlagForPerson, eksplisitteAvslag ->
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
