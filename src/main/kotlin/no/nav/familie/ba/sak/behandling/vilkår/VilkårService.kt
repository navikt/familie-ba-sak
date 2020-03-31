package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriodeResultat
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val loggService: LoggService
) {

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barn = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barn.size > 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse inneholder kan kun inneholde ett barn, men inneholder ${barn.size}")
        }

        val periodeResultater = personopplysningGrunnlag.personer.map { person ->
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person)
            val evaluering = spesifikasjonerForPerson.evaluer(
                    Fakta(personForVurdering = person)
            )
            val resultaterForPerson = evaluering.children.map { child ->
                VilkårResultat(resultat = child.resultat,
                               vilkårType = Vilkår.valueOf(child.identifikator))
            }.toSet()
            PeriodeResultat(personIdent = person.personIdent.ident,
                            vilkårResultater = resultaterForPerson,
                            periodeFom = barn.first().fødselsdato.plusMonths(1),
                            periodeTom = barn.first().fødselsdato.plusYears(18).minusMonths(1))
        }.toSet()

        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true,
                periodeResultater = periodeResultater)

        lagreNyOgDeaktiverGammelBehandlingResultat(behandlingResultat)
        return behandlingResultat
    }

    fun kontrollerVurderteVilkårOgLagResultat(periodeResultater: List<RestPeriodeResultat>,
                                              behandlingId: Long): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.periodeResultater = periodeResultater.map { restPeriodeResultat ->
            val periodeResultat = PeriodeResultat(personIdent = restPeriodeResultat.personIdent,
                                                  periodeFom = restPeriodeResultat.periodeFom,
                                                  periodeTom = restPeriodeResultat.periodeTom
            )
            periodeResultat.vilkårResultater = restPeriodeResultat.vilkårResultater?.map { restVilkårResultat ->
                VilkårResultat(
                        periodeResultat = periodeResultat,
                        vilkårType = restVilkårResultat.vilkårType,
                        resultat = restVilkårResultat.resultat
                )
            }?.toSet() ?: setOf()

            periodeResultat
        }.toSet()

        lagreNyOgDeaktiverGammelBehandlingResultat(behandlingResultat)
        return behandlingResultat
    }

    private fun spesifikasjonerForPerson(person: Person): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)

        return relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
    }

    private fun lagreNyOgDeaktiverGammelBehandlingResultat(nyttBehandlingResultat: BehandlingResultat) {
        val behandlingResultatSomSettesInaktivt =
                behandlingResultatRepository.findByBehandlingAndAktiv(nyttBehandlingResultat.behandling.id)

        if (behandlingResultatSomSettesInaktivt != null) {
            behandlingResultatSomSettesInaktivt.aktiv = false
            behandlingResultatRepository.save(behandlingResultatSomSettesInaktivt)
        }

        val behandling = behandlingService.hent(nyttBehandlingResultat.behandling.id)
        loggService.opprettVilkårsvurderingLogg(behandling, behandlingResultatSomSettesInaktivt, nyttBehandlingResultat)

        behandlingResultatRepository.save(nyttBehandlingResultat)
    }
}