package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val samletVilkårResultatRepository: SamletVilkårResultatRepository,
        private val loggService: LoggService
) {

    fun lagreNyOgDeaktiverGammelSamletVilkårResultat(periodeResultat: PeriodeResultat) {
        val aktivSamletVilkårResultat =
                samletVilkårResultatRepository.finnSamletVilkårResultatPåBehandlingOgAktiv(periodeResultat.behandlingId)

        if (aktivSamletVilkårResultat != null) {
            aktivSamletVilkårResultat.aktiv = false
            samletVilkårResultatRepository.save(aktivSamletVilkårResultat)
        }

        val behandling = behandlingService.hent(periodeResultat.behandlingId)
        loggService.opprettVilkårsvurderingLogg(behandling, aktivSamletVilkårResultat, periodeResultat)

        samletVilkårResultatRepository.save(periodeResultat)
    }

    fun vurderVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                  behandlingId: Long): PeriodeResultat {
        val resultatForSak = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val tmpFakta = Fakta(personForVurdering = person)
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person, behandlingId)
            val evaluering = spesifikasjonerForPerson.evaluer(tmpFakta)
            evaluering.children.map { child ->
                resultatForSak.add(VilkårResultat(person = person,
                                                     resultat = child.resultat,
                                                     vilkårType = Vilkår.valueOf(child.identifikator)))
            }
        }
        val samletVilkårResultat = PeriodeResultat(periodeResultat = resultatForSak, behandlingId = behandlingId)
        resultatForSak.map { it.periodeResultat = samletVilkårResultat }
        lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat)
        return samletVilkårResultat
    }

    fun kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                  restSamletVilkårResultat: List<RestVilkårResultat>,
                                  behandlingId: Long): PeriodeResultat {
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
        val samletVilkårResultat = PeriodeResultat(periodeResultat = listeAvVilkårResultat, behandlingId = behandlingId)
        listeAvVilkårResultat.map { it.periodeResultat = samletVilkårResultat }
        lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat)
        return samletVilkårResultat

}

    fun spesifikasjonerForPerson(person: Person, behandlingId: Long): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)
        val samletSpesifikasjon = relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
        return samletSpesifikasjon
    }
}