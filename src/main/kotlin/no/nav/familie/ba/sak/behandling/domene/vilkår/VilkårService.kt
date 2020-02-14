package no.nav.familie.ba.sak.behandling.domene.vilkår

import javassist.NotFoundException
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val samletVilkårResultatRepository: SamletVilkårResultatRepository
) {

    fun vurderVilkår(personopplysningGrunnlag: PersonopplysningGrunnlag,
                     restSamletVilkårResultat: List<RestVilkårResultat>): SamletVilkårResultat {
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()

        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restSamletVilkårResultat.filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
            val vilkårForPart = VilkårType.hentVilkårForPart(person.type)

            vilkårForPerson.forEach {
                vilkårForPart.find { vilkårType -> vilkårType == it.vilkårType }
                ?: throw NotFoundException("Vilkåret $it finnes ikke i grunnlaget for parten $vilkårForPart")

                listeAvVilkårResultat.add(VilkårResultat(vilkårType = it.vilkårType,
                                                         utfallType = it.utfallType,
                                                         person = person))
            }

            if (listeAvVilkårResultat.filter { it.person.personIdent.ident == person.personIdent.ident }.size != vilkårForPart.size) {
                throw IllegalStateException("Vilkårene for ${person.type} er ${vilkårForPerson.map { v -> v.vilkårType }}, men vi forventer $vilkårForPart")
            }
        }
        val samletVilkårResultat = SamletVilkårResultat(samletVilkårResultat = listeAvVilkårResultat)
        listeAvVilkårResultat.map { it.samletVilkårResultat = samletVilkårResultat }

        samletVilkårResultatRepository.save(samletVilkårResultat)

        return samletVilkårResultat
    }
}