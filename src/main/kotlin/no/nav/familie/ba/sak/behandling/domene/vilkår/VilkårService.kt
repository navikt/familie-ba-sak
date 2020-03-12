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
                                  restSamletVilkårResultat: List<RestVilkårResultat>,
                                  behandlingId: Long): SamletVilkårResultat {
        val tmpFakta = Fakta(personopplysningGrunnlag = personopplysningGrunnlag)//Bør være pakket inn i fakta tidligere
        val resultatForSak = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person, behandlingId)
            val evaluering = spesifikasjonerForPerson.evaluer(tmpFakta)
            val resultatForPerson = mutableSetOf<VilkårResultat>()
            evaluering.children.map { child ->
                resultatForPerson.add(VilkårResultat(person = person,
                                                     resultat = child.resultat,
                                                     vilkårType = Vilkår.valueOf(child.identifikator)))
            }
            resultatForSak.addAll(resultatForPerson)
        }
        val samletVilkårResultat = SamletVilkårResultat(samletVilkårResultat = resultatForSak, behandlingId = behandlingId)
        resultatForSak.map { it.samletVilkårResultat = samletVilkårResultat }

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