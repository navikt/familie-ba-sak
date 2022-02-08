package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun validerIngenVilkårSattEtterSøkersDød(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,

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
