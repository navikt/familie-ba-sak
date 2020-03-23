package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonVilkårResultat
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val loggService: LoggService,
        private val periodeService: PeriodeService
) {

    fun lagreNyOgDeaktiverGammelBehandlingResultat(behandlingResultat: BehandlingResultat) {
        val aktivtBehandlingResultat =
                behandlingResultatRepository.findByBehandlingAndAktiv(behandlingResultat.behandling.id)

        if (aktivtBehandlingResultat != null) {
            aktivtBehandlingResultat.aktiv = false
            behandlingResultatRepository.save(aktivtBehandlingResultat)
        }

        val behandling = behandlingService.hent(behandlingResultat.behandling.id)
        loggService.opprettVilkårsvurderingLogg(behandling, aktivtBehandlingResultat, behandlingResultat)

        behandlingResultatRepository.save(behandlingResultat)
    }

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barn = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barn.size > 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse inneholder kan kun inneholde et barn, men inneholder ${barn.size}")
        }
        val periodeResultater = mutableSetOf<PeriodeResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val resultaterForPerson = mutableSetOf<VilkårResultat>()
            val tmpFakta = Fakta(personForVurdering = person)
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person)
            val evaluering = spesifikasjonerForPerson.evaluer(tmpFakta)
            evaluering.children.map { child ->
                resultaterForPerson.add(VilkårResultat(person = person,
                                                       resultat = child.resultat,
                                                       vilkårType = Vilkår.valueOf(child.identifikator)))
            }
            periodeResultater.add(PeriodeResultat(vilkårResultater = resultaterForPerson,
                                                  periodeFom = barn.first().fødselsdato.plusMonths(1),
                                                  periodeTom = barn.first().fødselsdato.plusYears(18).minusMonths(1)))
        }
        val behandlingResultat = BehandlingResultat(
                id = behandlingId,
                behandling = behandlingRepository.finnBehandling(behandlingId),
                aktiv = true,
                periodeResultater = periodeResultater)
        lagreNyOgDeaktiverGammelBehandlingResultat(behandlingResultat)
        return behandlingResultat
    }

    fun kontrollerVurderteVilkårOgLagResultat(restBehandlingResultat: List<RestPersonVilkårResultat>,
                                              behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val listeAvVilkårResultat = mutableSetOf<VilkårResultat>()
        personopplysningGrunnlag.personer.map { person ->
            val vilkårForPerson = restBehandlingResultat
                    .filter { vilkår -> vilkår.personIdent == person.personIdent.ident }
                    .firstOrNull()
                    ?.vurderteVilkår ?: error("Fant ingen vurderte vilkår for person")
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

        val periodeResultater =
                restBehandlingResultat.map { personResultat -> periodeService.restPersonVilkårTilPerioder(personResultat) }
                        .flatten()
                        .toMutableSet()
        val behandlingResultat = BehandlingResultat(
                id = behandlingId,
                behandling = behandlingRepository.finnBehandling(behandlingId),
                aktiv = true,
                periodeResultater = periodeResultater) //TODO: Oppdater med mappet behandlingsresultat
        lagreNyOgDeaktiverGammelBehandlingResultat(behandlingResultat)
        return behandlingResultat

    }

    fun spesifikasjonerForPerson(person: Person): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)
        val samletSpesifikasjon = relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
        return samletSpesifikasjon
    }
}