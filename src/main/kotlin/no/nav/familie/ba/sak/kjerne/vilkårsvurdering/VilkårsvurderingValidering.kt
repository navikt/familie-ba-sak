package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.harBlandetRegelverk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun validerIngenVilkårSattEtterSøkersDød(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering

) {
    val vilkårResultaterSøker =
        vilkårsvurdering.hentPersonResultaterTil(personopplysningGrunnlag.søker.aktør.aktørId)
    val søkersDød = personopplysningGrunnlag.søker.dødsfall?.dødsfallDato ?: LocalDate.now()

    val vilkårSomEnderEtterSøkersDød =
        vilkårResultaterSøker
            .groupBy { it.vilkårType }
            .mapNotNull { (vilkårType, vilkårResultater) ->
                vilkårType.takeIf {
                    vilkårResultater.any {
                        it.periodeTom?.isAfter(søkersDød) ?: true
                    }
                }
            }

    if (vilkårSomEnderEtterSøkersDød.isNotEmpty()) {
        throw FunksjonellFeil(
            "Ved behandlingsårsak \"Dødsfall Bruker\" må vilkårene på søker avsluttes " +
                "senest dagen søker døde, men " +
                Utils.slåSammen(vilkårSomEnderEtterSøkersDød.map { "\"" + it.beskrivelse + "\"" }) +
                " vilkåret til søker slutter etter søkers død."
        )
    }
}

fun validerIkkeBlandetRegelverk(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering
) {
    val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(vilkårsvurdering, personopplysningGrunnlag)
    if (vilkårsvurderingTidslinjer.harBlandetRegelverk()) {
        throw FunksjonellFeil(
            melding = "Det er forskjellig regelverk for en eller flere perioder for søker eller barna"
        )
    }
}

fun valider18ÅrsVilkårEksistererFraFødselsdato(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering
) {
    vilkårsvurdering.personResultater.forEach { personResultat ->
        val person = personopplysningGrunnlag.personer.find { it.aktør == personResultat.aktør }
        if (person?.type == PersonType.BARN && !personResultat.vilkårResultater.finnesUnder18VilkårFraFødselsdato(person.fødselsdato)) {
            throw FunksjonellFeil(
                melding = "Barn født ${person.fødselsdato} har ikke fått under 18-vilkåret vurdert fra fødselsdato",
                frontendFeilmelding = "Det må være en periode på 18-års vilkåret som starter på barnets fødselsdato"
            )
        }
    }
}

private fun Set<VilkårResultat>.finnesUnder18VilkårFraFødselsdato(fødselsdato: LocalDate): Boolean = this.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.any { it.periodeFom == fødselsdato }
