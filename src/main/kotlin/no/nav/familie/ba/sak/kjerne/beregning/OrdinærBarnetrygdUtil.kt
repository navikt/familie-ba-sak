package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal

object OrdinærBarnetrygdUtil {

    internal fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        personResultater: Set<PersonResultat>,
        fagsakType: FagsakType,
    ): List<BeregnetAndel> {
        val tidslinjerMedRettTilProsentPerBarn =
            personResultater.lagTidslinjerMedRettTilProsentPerBarn(personopplysningGrunnlag, fagsakType)

        return tidslinjerMedRettTilProsentPerBarn.flatMap { (barn, tidslinjeMedRettTilProsentForBarn) ->
            val satsTidslinje = lagOrdinærTidslinje(barn)
            val satsProsentTidslinje = kombinerProsentOgSatsTidslinjer(tidslinjeMedRettTilProsentForBarn, satsTidslinje)

            satsProsentTidslinje.perioder().map {
                val innholdIPeriode = it.innhold
                    ?: throw Feil("Finner ikke sats og prosent i periode (${it.fraOgMed} - ${it.tilOgMed}) ved generering av andeler tilkjent ytelse")
                BeregnetAndel(
                    person = barn,
                    stønadFom = it.fraOgMed.tilYearMonth(),
                    stønadTom = it.tilOgMed.tilYearMonth(),
                    beløp = innholdIPeriode.sats.avrundetHeltallAvProsent(innholdIPeriode.prosent),
                    sats = innholdIPeriode.sats,
                    prosent = innholdIPeriode.prosent,
                    regelverk = innholdIPeriode.rettTilProsent.regelverk
                )
            }
        }
    }

    private fun kombinerProsentOgSatsTidslinjer(
        tidslinjeMedRettTilProsentForBarn: Tidslinje<RettTilProsent, Måned>,
        satsTidslinje: Tidslinje<Int, Måned>,
    ) = tidslinjeMedRettTilProsentForBarn.kombinerMed(satsTidslinje) { rettTilProsent, sats ->
        when {
            rettTilProsent == null -> null
            sats == null -> throw Feil("Finner ikke sats i periode med rett til utbetaling")
            else -> SatsProsent(sats, rettTilProsent)
        }
    }.slåSammenLike().filtrerIkkeNull()

    private data class SatsProsent(
        val sats: Int,
        val rettTilProsent: RettTilProsent,
    ) {
        val prosent: BigDecimal get() = rettTilProsent.prosent
    }

    data class RettTilProsent(
        val prosent: BigDecimal,
        val regelverk: Regelverk?,
    )

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerBarn(personopplysningGrunnlag: PersonopplysningGrunnlag, fagsakType: FagsakType): Map<Person, Tidslinje<RettTilProsent, Måned>> {
        val tidslinjerPerPerson = lagTidslinjerMedRettTilProsentPerPerson(personopplysningGrunnlag, fagsakType)

        if (tidslinjerPerPerson.isEmpty()) return emptyMap()

        val søkerTidslinje = tidslinjerPerPerson[personopplysningGrunnlag.søker] ?: return emptyMap()
        val barnasTidslinjer = tidslinjerPerPerson.filter { it.key in personopplysningGrunnlag.barna }

        return kombinerSøkerMedHvertBarnSinTidslinje(barnasTidslinjer, søkerTidslinje)
    }

    private fun kombinerSøkerMedHvertBarnSinTidslinje(
        barnasTidslinjer: Map<Person, Tidslinje<RettTilProsent, Måned>>,
        søkerTidslinje: Tidslinje<RettTilProsent, Måned>
    ) = barnasTidslinjer.mapValues { (_, barnTidslinje) ->
        barnTidslinje.kombinerMed(søkerTidslinje) { barnProsent, søkerProsent ->
            when {
                barnProsent == null || søkerProsent == null -> null
                else -> barnProsent
            }
        }.slåSammenLike().filtrerIkkeNull()
    }

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerPerson(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        fagsakType: FagsakType,
    ) = this.associate { personResultat ->
        val person = personopplysningGrunnlag.personer.find { it.aktør == personResultat.aktør }
            ?: throw Feil("Finner ikke person med aktørId=${personResultat.aktør.aktørId} i persongrunnlaget ved generering av andeler tilkjent ytelse")
        person to personResultat.tilTidslinjeMedRettTilProsentForPerson(
            person = person,
            fagsakType = fagsakType,
        )
    }

    internal fun PersonResultat.tilTidslinjeMedRettTilProsentForPerson(
        person: Person,
        fagsakType: FagsakType,
    ): Tidslinje<RettTilProsent, Måned> {
        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(person.fødselsdato)

        return tidslinjer.kombiner { it.mapTilProsentEllerNull(person.type, fagsakType) }.slåSammenLike().filtrerIkkeNull()
    }

    internal fun Iterable<VilkårResultat>.mapTilProsentEllerNull(personType: PersonType, fagsakType: FagsakType): RettTilProsent? {
        return if (alleOrdinæreVilkårErOppfylt(personType, fagsakType)) {
            if (any { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) }) {
                RettTilProsent(
                    prosent = BigDecimal(50),
                    regelverk = find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.vurderesEtter
                )
            } else {
                RettTilProsent(
                    prosent = BigDecimal(100),
                    regelverk = find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.vurderesEtter
                )
            }
        } else {
            null
        }
    }
}
