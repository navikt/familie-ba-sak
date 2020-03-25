package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriodeResultat
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val loggService: LoggService
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
                resultaterForPerson.add(VilkårResultat(resultat = child.resultat,
                                                       vilkårType = Vilkår.valueOf(child.identifikator)))
            }
            periodeResultater.add(PeriodeResultat(personIdent = person.personIdent.ident,
                                                  vilkårResultater = resultaterForPerson,
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

    fun kontrollerVurderteVilkårOgLagResultat(periodeResultater: List<RestPeriodeResultat>,
                                              behandlingId: Long): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                id = behandlingId,
                behandling = behandlingRepository.finnBehandling(behandlingId),
                aktiv = true)
        behandlingResultat.periodeResultater = periodeResultater.map {
            val periodeResultat = PeriodeResultat(personIdent = it.personIdent,
                                                  periodeFom = it.periodeFom,
                                                  periodeTom = it.periodeTom
            )
            PeriodeResultat(personIdent = it.personIdent,
                            periodeFom = it.periodeFom,
                            periodeTom = it.periodeTom,
                            vilkårResultater = it.vilkårResultater?.map { restVilkårResultat ->
                                VilkårResultat(
                                        periodeResultat = periodeResultat,
                                        vilkårType = restVilkårResultat.vilkårType,
                                        resultat = restVilkårResultat.resultat
                                )
                            }?.toMutableSet() ?: mutableSetOf())
        }.toMutableSet()
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