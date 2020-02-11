package no.nav.familie.ba.sak.behandling.domene.vilkår

import javassist.NotFoundException
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import org.springframework.stereotype.Service

@Service
class VilkårService {
    fun vurderVilkår(personopplysningGrunnlag: PersonopplysningGrunnlag,
                     restSamletVilkårResultat: List<RestVilkårResultat>): SamletVilkårResultat {
        val samletVilkårResultat = SamletVilkårResultat()
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()

        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restSamletVilkårResultat.filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
            val vilkårForPart = VilkårType.hentVilkårForPart(person.type)

            vilkårForPerson.forEach {
                vilkårForPart.find { vilkårType -> vilkårType == it.vilkårType }
                ?: throw NotFoundException("Vilkåret $it finnes ikke i grunnlaget for parten $vilkårForPart")

                listeAvVilkårResultat.add(VilkårResultat(samletVilkårResultat = samletVilkårResultat,
                                                         vilkårType = it.vilkårType,
                                                         utfallType = it.utfallType,
                                                         person = person))
            }

            if (listeAvVilkårResultat.filter { it.person.personIdent.ident == person.personIdent.ident }.size != vilkårForPart.size) {
                throw IllegalStateException("Vilkårene for ${person.type} er ${vilkårForPerson.map { v -> v.vilkårType }}, men vi forventer $vilkårForPart")
            }
        }
        samletVilkårResultat.samletVilkårResultat = listeAvVilkårResultat

        return samletVilkårResultat
    }
}