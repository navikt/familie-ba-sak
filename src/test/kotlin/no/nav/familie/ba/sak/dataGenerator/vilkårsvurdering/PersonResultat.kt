package no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun lagPersonResultatAvOverstyrteResultater(
    person: Person,
    overstyrendeVilkårResultater: List<VilkårResultat>,
    vilkårsvurdering: Vilkårsvurdering,

): PersonResultat {
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = person.aktør
    )

    val erUtvidet = overstyrendeVilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

    val vilkårResultater = Vilkår.hentVilkårFor(
        personType = person.type,
        ytelseType = if (erUtvidet) YtelseType.UTVIDET_BARNETRYGD else YtelseType.ORDINÆR_BARNETRYGD
    ).map { vilkårType ->
        overstyrendeVilkårResultater
            .find { it.vilkårType == vilkårType }
            ?: VilkårResultat(
                personResultat = personResultat,
                periodeFom = if (vilkårType == Vilkår.UNDER_18_ÅR) person.fødselsdato else maxOf(
                    person.fødselsdato,
                    LocalDate.now().minusYears(3)
                ),
                periodeTom = if (vilkårType == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18) else null,
                vilkårType = vilkårType,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList()
            )
    }.toSet()

    personResultat.setSortedVilkårResultater(vilkårResultater)

    return personResultat
}
