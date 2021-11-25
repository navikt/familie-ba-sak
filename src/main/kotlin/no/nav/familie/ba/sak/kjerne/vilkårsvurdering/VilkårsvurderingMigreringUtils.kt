package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

object VilkårsvurderingMigreringUtils {

    fun hentForrigeVilkårsvurderingVilkårResultater(
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person
    ): List<VilkårResultat> {
        val personResultat = forrigeBehandlingsvilkårsvurdering.personResultater
            .first { it.personIdent == person.personIdent.ident }
        return personResultat.vilkårResultater
            .filter { it.vilkårType == vilkår }
    }

    fun utledPeriodeFom(
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person,
        nyMigreringsdato: LocalDate
    ): LocalDate {
        val forrigeVilkårsPeriodeFom = hentForrigeVilkårsvurderingVilkårResultater(
            forrigeBehandlingsvilkårsvurdering,
            vilkår, person
        ).minOf { it.periodeFom!! }
        return when {
            person.fødselsdato.isAfter(nyMigreringsdato) ||
                vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato
            forrigeVilkårsPeriodeFom.isBefore(nyMigreringsdato) -> forrigeVilkårsPeriodeFom
            else -> nyMigreringsdato
        }
    }

    fun utledPeriodeTom(
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person,
        periodeFom: LocalDate,
    ): LocalDate? {
        val forrigeVilkårsPeriodeTom: LocalDate? = hentForrigeVilkårsvurderingVilkårResultater(
            forrigeBehandlingsvilkårsvurdering, vilkår, person
        ).minWithOrNull(VilkårResultat.VilkårResultatComparator)?.periodeTom
        return when {
            vilkår == Vilkår.UNDER_18_ÅR -> periodeFom.plusYears(18).minusDays(1)
            vilkår == Vilkår.GIFT_PARTNERSKAP -> null
            forrigeVilkårsPeriodeTom != null -> forrigeVilkårsPeriodeTom
            else -> null
        }
    }

    fun kopiManglendePerioderFraForrigeVilkårsvurdering(
        vilkårResulater: Set<VilkårResultat>,
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        person: Person
    ): List<VilkårResultat> {
        val manglendeVilkårResultater = mutableListOf<VilkårResultat>()
        vilkårResulater.forEach {
            val forrigeVilkårResultater =
                hentForrigeVilkårsvurderingVilkårResultater(forrigeBehandlingsvilkårsvurdering, it.vilkårType, person)
            manglendeVilkårResultater.addAll(
                forrigeVilkårResultater.filter { forrigeVilkårResultat ->
                    forrigeVilkårResultat.periodeFom != it.periodeFom &&
                        forrigeVilkårResultat.periodeTom != it.periodeTom
                }
            )
        }
        return manglendeVilkårResultater
    }
}
