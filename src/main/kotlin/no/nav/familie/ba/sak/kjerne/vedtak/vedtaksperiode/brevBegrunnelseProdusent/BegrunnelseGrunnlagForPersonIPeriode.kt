package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.EndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.KompetanseForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.OvergangsstønadForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.filtrerPåAktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.hentErUtbetalingSmåbarnstilleggTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.tilAndelerForVedtaksPeriodeTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer

data class BegrunnelseGrunnlagForPersonIPeriode(
    val person: Person,
    val vilkårResultater: Iterable<VilkårResultatForVedtaksperiode>,
    val andeler: Iterable<AndelForVedtaksperiode>,
    val kompetanse: KompetanseForVedtaksperiode? = null,
    val endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode? = null,
    val overgangsstønad: OvergangsstønadForVedtaksperiode? = null,
)

fun BehandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagForPersonIPeriode(person: Person): Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned> {
    val forskjøvedeVilkårResultaterForPerson =
        this.personResultater.single { it.aktør == person.aktør }
            .vilkårResultater.tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
            .map { tidslinje -> tidslinje.map { it?.let { VilkårResultatForVedtaksperiode(it) } } }
            .kombiner { it }

    val kompetanseTidslinje = this.utfylteKompetanser.filtrerPåAktør(person.aktør)
        .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

    val endredeUtbetalingerTidslinje = this.utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
        .tilTidslinje().mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

    val andelerTilkjentYtelseTidslinje =
        this.andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje()

    val overgangsstønadTidslinje =
        this.perioderOvergangsstønad.filtrerPåAktør(person.aktør)
            .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(andelerTilkjentYtelseTidslinje.hentErUtbetalingSmåbarnstilleggTidslinje())

    return forskjøvedeVilkårResultaterForPerson
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
            grunnlagForPerson?.let { grunnlagForPerson.copy(kompetanse = kompetanse) }
        }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
            grunnlagForPerson?.let { grunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel) }
        }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
            grunnlagForPerson?.let { grunnlagForPerson.copy(overgangsstønad = overgangsstønad) }
        }
}
