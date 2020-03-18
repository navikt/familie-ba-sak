package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val periodeResultatRepository: PeriodeResultatRepository,
        private val loggService: LoggService
) {

    fun lagreNyOgDeaktiverGammelPeriodeResultat(periodeResultat: PeriodeResultat) {
        val aktivtPeriodeResultat =
                periodeResultatRepository.finnPeriodeResultatPåBehandlingOgAktiv(periodeResultat.behandlingId)

        if (aktivtPeriodeResultat != null) {
            aktivtPeriodeResultat.aktiv = false
            periodeResultatRepository.save(aktivtPeriodeResultat)
        }

        val behandling = behandlingService.hent(periodeResultat.behandlingId)
        loggService.opprettVilkårsvurderingLogg(behandling, aktivtPeriodeResultat, periodeResultat)

        periodeResultatRepository.save(periodeResultat)
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
        val periodeResultat = PeriodeResultat(periodeResultat = resultatForSak, behandlingId = behandlingId, periodeFom = LocalDate.now(), periodeTom = LocalDate.now()) //TODO: Oppdater med periode
        resultatForSak.map { it.periodeResultat = periodeResultat }
        lagreNyOgDeaktiverGammelPeriodeResultat(periodeResultat)
        return periodeResultat
    }

    fun kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                              restPeriodeResultat: List<RestVilkårResultat>,
                                              behandlingId: Long): PeriodeResultat {
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restPeriodeResultat.filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
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
        val periodeResultat = PeriodeResultat(periodeResultat = listeAvVilkårResultat, behandlingId = behandlingId, periodeFom = LocalDate.now(), periodeTom = LocalDate.now()) //TODO: Oppdater med periode
        listeAvVilkårResultat.map { it.periodeResultat = periodeResultat }
        lagreNyOgDeaktiverGammelPeriodeResultat(periodeResultat)
        return periodeResultat

}

    fun spesifikasjonerForPerson(person: Person, behandlingId: Long): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)
        val samletSpesifikasjon = relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
        return samletSpesifikasjon
    }
}