package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun validerSøkerBosattIRiketTomdato(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,

) {
    val vilkårResultaterSøker =
        vilkårsvurdering.hentPersonResultaterTil(personopplysningGrunnlag.søker.aktør.aktørId)
    val søkerBorIRiketTom =
        vilkårResultaterSøker
            .filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            .maxByOrNull { it.periodeTom ?: TIDENES_ENDE }?.periodeTom

    if (søkerBorIRiketTom == null) {
        throw FunksjonellFeil(
            "Ved behandlingsårsak \"Dødsfall Bruker\" må bosatt i riket vilkåret avsluttes " +
                "dagen søker døde, men bosatt i riket vilkåret har ingen til og med dato"
        )
    }

    if (søkerBorIRiketTom.isAfter(LocalDate.now())) {
        throw FunksjonellFeil(
            "Ved behandlingsårsak \"Dødsfall Bruker\" må bosatt i riket vilkåret avsluttes " +
                "dagen søker døde, men til og med datoen er satt til ${søkerBorIRiketTom.tilKortString()} "
        )
    }
}
