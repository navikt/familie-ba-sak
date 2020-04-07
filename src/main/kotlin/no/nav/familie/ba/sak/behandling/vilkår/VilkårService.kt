package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPeriodeResultat
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barn = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barn.size > 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse inneholder kan kun inneholde ett barn, men inneholder ${barn.size}")
        }


        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.periodeResultater = personopplysningGrunnlag.personer.map { person ->
            val periodeResultat = PeriodeResultat(behandlingResultat = behandlingResultat,
                                                  personIdent = person.personIdent.ident,
                                                  periodeFom = barn.first().fødselsdato.plusMonths(1),
                                                  periodeTom = barn.first().fødselsdato.plusYears(18).minusMonths(1))
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person)
            val evaluering = spesifikasjonerForPerson.evaluer(
                    Fakta(personForVurdering = person)
            )
            periodeResultat.vilkårResultater = evaluering.children.map { child ->
                VilkårResultat(periodeResultat = periodeResultat,
                               resultat = child.resultat,
                               vilkårType = Vilkår.valueOf(child.identifikator),
                               begrunnelse = "")

            }.toSet()
            periodeResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
    }

    fun initierVilkårForManuellBahandling(behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.periodeResultater = personopplysningGrunnlag.personer.map { person ->
            val periodeResultat = PeriodeResultat(behandlingResultat = behandlingResultat,
                                                  personIdent = person.personIdent.ident)

            val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)
            periodeResultat.vilkårResultater = relevanteVilkår.map { vilkår ->
                VilkårResultat(periodeResultat = periodeResultat,
                               resultat = Resultat.KANSKJE,
                               vilkårType = vilkår,
                               begrunnelse = "")
            }.toSet()
            periodeResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
    }

     fun kontrollerVurderteVilkårOgLagResultat(periodeResultater: List<RestPeriodeResultat>,
                                              begrunnelse: String,
                                              behandlingId: Long): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.periodeResultater = periodeResultater.map { restPeriodeResultat ->
            val periodeResultat = PeriodeResultat(behandlingResultat = behandlingResultat,
                                                  personIdent = restPeriodeResultat.personIdent,
                                                  periodeFom = restPeriodeResultat.periodeFom,
                                                  periodeTom = restPeriodeResultat.periodeTom
            )
            periodeResultat.vilkårResultater = restPeriodeResultat.vilkårResultater?.map { restVilkårResultat ->
                VilkårResultat(
                        periodeResultat = periodeResultat,
                        vilkårType = restVilkårResultat.vilkårType,
                        resultat = restVilkårResultat.resultat,
                        begrunnelse = begrunnelse
                )
            }?.toSet() ?: setOf()

            periodeResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
    }

    private fun spesifikasjonerForPerson(person: Person): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.VILKÅRGJELDERFOR)

        return relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
    }
}