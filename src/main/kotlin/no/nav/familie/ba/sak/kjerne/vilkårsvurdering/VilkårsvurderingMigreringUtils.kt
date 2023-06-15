package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
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

    fun kopiManglendePerioderFraForrigeVilkårsvurdering(
        vilkårResulater: Set<VilkårResultat>,
        forrigeBehandlingsvilkårsvurdering: Vilkårsvurdering,
        person: Person,
        personResultat: PersonResultat,
    ): List<VilkårResultat> {
        val manglendeVilkårResultater = mutableListOf<VilkårResultat>()
        vilkårResulater.forEach {
            val forrigeVilkårResultater =
                hentVilkårResultaterSomErOppfyltFraForrigeVilkårsvurdering(
                    forrigeBehandlingsvilkårsvurdering,
                    it.vilkårType,
                    person,
                )
            manglendeVilkårResultater.addAll(
                forrigeVilkårResultater.filter { forrigeVilkårResultat ->
                    forrigeVilkårResultat.periodeFom != it.periodeFom &&
                        forrigeVilkårResultat.periodeTom != it.periodeTom
                }.map { vilkårResultat -> vilkårResultat.kopierMedParent(personResultat) }
                    .toSet(), // Mulig vi her burde bruke vilkårResultat.tilKopiForNyttPersonResultat slik at behandlingsId blir oppdatert.
            )
        }
        return manglendeVilkårResultater
    }

    fun finnEksisterendeVilkårResultatSomBlirForskjøvet(
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
        person: Person,
        nyFom: LocalDate,
        nyTom: LocalDate?,
    ) =
        hentVilkårResultaterSomErOppfyltFraForrigeVilkårsvurdering(forrigeBehandlingVilkårsvurdering, vilkår, person)
            .single { it.periodeFom == nyFom || it.periodeTom == nyTom || (it.periodeFom!! > nyFom && nyTom == null) }

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
