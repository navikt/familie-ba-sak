package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val samletVilkårResultatRepository: SamletVilkårResultatRepository
) {

    fun lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat: SamletVilkårResultat) {
        val aktivSamletVilkårResultat =
                samletVilkårResultatRepository.finnSamletVilkårResultatPåBehandlingOgAktiv(samletVilkårResultat.behandlingId)

        if (aktivSamletVilkårResultat != null) {
            aktivSamletVilkårResultat.aktiv = false
            samletVilkårResultatRepository.save(aktivSamletVilkårResultat)
        }

        samletVilkårResultatRepository.save(samletVilkårResultat)
    }

    fun vurderVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                  behandlingId: Long): SamletVilkårResultat {
        val resultatForSak = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val tmpFakta = Fakta(personForVurdering = person)
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person, behandlingId)
            val evaluering = spesifikasjonerForPerson.evaluer(tmpFakta)
            val resultatForPerson = mutableSetOf<VilkårResultat>()
            evaluering.children.map { child ->
                resultatForPerson.add(VilkårResultat(person = person,
                                                     resultat = child.resultat,
                                                     vilkårType = Vilkår.valueOf(child.identifikator)))
                resultatForSak.addAll(resultatForPerson)
            }
        }
        val samletVilkårResultat = SamletVilkårResultat(samletVilkårResultat = resultatForSak, behandlingId = behandlingId)
        resultatForSak.map { it.samletVilkårResultat = samletVilkårResultat }
        lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat)
        return samletVilkårResultat
    }

    fun kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                  restSamletVilkårResultat: List<RestVilkårResultat>,
                                  behandlingId: Long): SamletVilkårResultat {
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restSamletVilkårResultat.filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
            val vilkårForPart = Vilkår.hentVilkårForPart(person.type)
            vilkårForPerson.forEach {
                vilkårForPart.find { vilkårType -> vilkårType == it.vilkårType }
                ?: error("Vilkåret $it finnes ikke i grunnlaget for parten $vilkårForPart")

                listeAvVilkårResultat.add(VilkårResultat(vilkårType = it.vilkårType,
                                                         resultat = it.resultat,
                                                         person = person))
            }
            if (listeAvVilkårResultat.filter { it.person.personIdent.ident == person.personIdent.ident }.size != vilkårForPart.size) {
                throw IllegalStateException("Vilkårene for ${person.type} er ${vilkårForPerson.map { v -> v.vilkårType }}, men vi forventer $vilkårForPart")
            }
        }
        val samletVilkårResultat = SamletVilkårResultat(samletVilkårResultat = listeAvVilkårResultat, behandlingId = behandlingId)
        listeAvVilkårResultat.map { it.samletVilkårResultat = samletVilkårResultat }
        lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat)
        return samletVilkårResultat

}

    fun spesifikasjonerForPerson(person: Person, behandlingId: Long): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, "TESTSAKSTYPE")
        val samletSpesifikasjon = relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
        return samletSpesifikasjon
    }
}