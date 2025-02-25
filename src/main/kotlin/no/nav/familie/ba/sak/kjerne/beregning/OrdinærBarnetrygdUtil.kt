package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvetTidslinjerForHvertOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
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

            satsProsentTidslinje.tilPerioderIkkeNull().map {
                val innholdIPeriode = it.verdi
                BeregnetAndel(
                    person = barn,
                    stønadFom = it.fom?.toYearMonth() ?: throw Feil("Fra og med-dato kan ikke være null"),
                    stønadTom = it.tom?.toYearMonth() ?: throw Feil("Til og med-dato kan ikke være null"),
                    beløp = innholdIPeriode.sats.avrundetHeltallAvProsent(innholdIPeriode.prosent),
                    sats = innholdIPeriode.sats,
                    prosent = innholdIPeriode.prosent,
                )
            }
        }
    }

    private fun kombinerProsentOgSatsTidslinjer(
        tidslinjeMedRettTilProsentForBarn: Tidslinje<BigDecimal>,
        satsTidslinje: Tidslinje<Int>,
    ) = tidslinjeMedRettTilProsentForBarn
        .kombinerMed(satsTidslinje) { rettTilProsent, sats ->
            when {
                rettTilProsent == null -> null
                sats == null -> throw Feil("Finner ikke sats i periode med rett til utbetaling")
                else -> SatsProsent(sats, rettTilProsent)
            }
        }.slåSammenLikePerioder()
        .filtrerIkkeNull()

    private data class SatsProsent(
        val sats: Int,
        val prosent: BigDecimal,
    )

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerBarn(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        fagsakType: FagsakType,
    ): Map<Person, Tidslinje<BigDecimal>> {
        val tidslinjerPerPerson = lagTidslinjerMedRettTilProsentPerPerson(personopplysningGrunnlag, fagsakType)

        if (tidslinjerPerPerson.isEmpty()) return emptyMap()

        val søkerTidslinje = tidslinjerPerPerson[personopplysningGrunnlag.søker] ?: return emptyMap()
        val barnasTidslinjer = tidslinjerPerPerson.filter { it.key in personopplysningGrunnlag.barna }

        return kombinerSøkerMedHvertBarnSinTidslinje(barnasTidslinjer, søkerTidslinje)
    }

    private fun kombinerSøkerMedHvertBarnSinTidslinje(
        barnasTidslinjer: Map<Person, Tidslinje<BigDecimal>>,
        søkerTidslinje: Tidslinje<BigDecimal>,
    ) = barnasTidslinjer.mapValues { (_, barnTidslinje) ->
        barnTidslinje
            .kombinerMed(søkerTidslinje) { barnProsent, søkerProsent ->
                when {
                    barnProsent == null || søkerProsent == null -> null
                    else -> barnProsent
                }
            }.slåSammenLikePerioder()
            .filtrerIkkeNull()
    }

    private fun Set<PersonResultat>.lagTidslinjerMedRettTilProsentPerPerson(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        fagsakType: FagsakType,
    ) = this.associate { personResultat ->
        val person =
            personopplysningGrunnlag.personer.find { it.aktør == personResultat.aktør }
                ?: throw Feil("Finner ikke person med aktørId=${personResultat.aktør.aktørId} i persongrunnlaget ved generering av andeler tilkjent ytelse")
        person to
            personResultat.tilTidslinjeMedRettTilProsentForPerson(
                person = person,
                fagsakType = fagsakType,
            )
    }

    internal fun PersonResultat.tilTidslinjeMedRettTilProsentForPerson(
        person: Person,
        fagsakType: FagsakType,
    ): Tidslinje<BigDecimal> {
        val tidslinjer = vilkårResultater.tilForskjøvetTidslinjerForHvertOppfylteVilkår(person.fødselsdato)

        return tidslinjer.kombiner { it.mapTilProsentEllerNull(person.type, fagsakType) }.slåSammenLikePerioder().filtrerIkkeNull()
    }

    internal fun Iterable<VilkårResultat>.mapTilProsentEllerNull(
        personType: PersonType,
        fagsakType: FagsakType,
    ): BigDecimal? =
        if (alleOrdinæreVilkårErOppfylt(personType, fagsakType)) {
            if (any { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) }) {
                BigDecimal(50)
            } else {
                BigDecimal(100)
            }
        } else {
            null
        }
}
