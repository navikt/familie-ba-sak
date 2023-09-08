package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

object VilkårsvurderingMigreringUtils {

    fun utledPeriodeFom(
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person,
        nyMigreringsdato: LocalDate,
    ): LocalDate {
        val forrigeVilkårsPeriodeFom = hentVilkårResultaterSomErOppfyltFraForrigeVilkårsvurdering(
            forrigeBehandlingsvilkårsvurdering,
            vilkår,
            person,
        ).minWithOrNull(VilkårResultat.VilkårResultatComparator)?.periodeFom
        return when {
            person.fødselsdato.isAfter(nyMigreringsdato) ||
                vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato

            forrigeVilkårsPeriodeFom != null &&
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
        val forrigeVilkårsPeriodeTom: LocalDate? = hentVilkårResultaterSomErOppfyltFraForrigeVilkårsvurdering(
            forrigeBehandlingsvilkårsvurdering,
            vilkår,
            person,
        ).minWithOrNull(VilkårResultat.VilkårResultatComparator)?.periodeTom
        return when {
            vilkår == Vilkår.UNDER_18_ÅR -> periodeFom.til18ÅrsVilkårsdato()
            vilkår == Vilkår.GIFT_PARTNERSKAP -> null
            forrigeVilkårsPeriodeTom != null -> forrigeVilkårsPeriodeTom
            else -> null
        }
    }

    fun finnManglendeOppfylteVilkårResultaterFraForrigeVilkårsvurdering(
        kopierteVilkårResultater: List<VilkårResultat>,
        oppfylteVilkårResultaterForPerson: List<VilkårResultat>,
    ): List<VilkårResultat> =
        oppfylteVilkårResultaterForPerson.filter {
            !kopierteVilkårResultater.any { kopiertVilkårResultat -> kopiertVilkårResultat.id == it.id }
        }

    private fun hentVilkårResultaterSomErOppfyltFraForrigeVilkårsvurdering(
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person,
    ): List<VilkårResultat> {
        val personResultat = forrigeBehandlingsvilkårsvurdering.personResultater
            .first { it.aktør == person.aktør }
        return personResultat.vilkårResultater
            .filter { it.vilkårType == vilkår && it.erOppfylt() }
    }
}
