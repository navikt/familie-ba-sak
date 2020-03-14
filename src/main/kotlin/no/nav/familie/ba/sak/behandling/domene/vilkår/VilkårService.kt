package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val samletVilkårResultatRepository: SamletVilkårResultatRepository,
        private val loggService: LoggService
) {

    fun lagreNyOgDeaktiverGammelSamletVilkårResultat(samletVilkårResultat: SamletVilkårResultat) {
        val aktivSamletVilkårResultat =
                samletVilkårResultatRepository.finnSamletVilkårResultatPåBehandlingOgAktiv(samletVilkårResultat.behandlingId)

        if (aktivSamletVilkårResultat != null) {
            aktivSamletVilkårResultat.aktiv = false
            samletVilkårResultatRepository.save(aktivSamletVilkårResultat)
        }

        val behandling = behandlingService.hent(samletVilkårResultat.behandlingId)
        loggService.opprettVilkårsvurderingLogg(behandling, aktivSamletVilkårResultat, samletVilkårResultat)

        samletVilkårResultatRepository.save(samletVilkårResultat)
    }

    fun vurderVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                  restSamletVilkårResultat: List<RestVilkårResultat>,
                                  behandlingId: Long): SamletVilkårResultat {
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()

        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restSamletVilkårResultat.filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
            val vilkårForPart = VilkårType.hentVilkårForPart(person.type)

            vilkårForPerson.forEach {
                vilkårForPart.find { vilkårType -> vilkårType == it.vilkårType }
                ?: error("Vilkåret $it finnes ikke i grunnlaget for parten $vilkårForPart")

                listeAvVilkårResultat.add(VilkårResultat(vilkårType = it.vilkårType,
                                                         utfallType = it.utfallType,
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
}