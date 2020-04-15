package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) {

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val behandling = behandlingService.hent(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barn = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barn.size > 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse inneholder kan kun inneholde ett barn, men inneholder ${barn.size}")
        }


        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.personResultater = personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                                personIdent = person.personIdent.ident)
            val spesifikasjonerForPerson = spesifikasjonerForPerson(person, behandling.kategori)
            val evaluering = spesifikasjonerForPerson.evaluer(
                    Fakta(personForVurdering = person)
            )
            val evalueringer = if (evaluering.children.isEmpty()) listOf(evaluering) else evaluering.children
            personResultat.vilkårResultater = vilkårResultater(personResultat, barn, evalueringer)
            personResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
    }

    fun initierVilkårvurderingForBehandling(behandlingId: Long): BehandlingResultat {
        val behandling = behandlingService.hent(behandlingId)
        val aktivBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId)
        if (aktivBehandlingResultat != null) {
            return aktivBehandlingResultat
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.personResultater = personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                                personIdent = person.personIdent.ident)

            val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.valueOfType(behandling.kategori))
            personResultat.vilkårResultater = relevanteVilkår.map { vilkår ->
                VilkårResultat(personResultat = personResultat,
                               resultat = Resultat.KANSKJE,
                               vilkårType = vilkår,
                               begrunnelse = "")
            }.toSet()
            personResultat
        }.toSet()

        return behandlingResultatService.lagre(behandlingResultat)
    }

    fun kontrollerVurderteVilkårOgLagResultat(personResultater: List<RestPersonResultat>,
                                              behandlingId: Long): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        behandlingResultat.personResultater = personResultater.map { restPersonResultat ->
            val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                                personIdent = restPersonResultat.personIdent
            )
            personResultat.vilkårResultater = restPersonResultat.vilkårResultater?.map { restVilkårResultat ->
                VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = restVilkårResultat.vilkårType,
                        resultat = restVilkårResultat.resultat,
                        periodeFom = restVilkårResultat.periodeFom,
                        periodeTom = restVilkårResultat.periodeTom,
                        begrunnelse = restVilkårResultat.begrunnelse
                )
            }?.toSet() ?: setOf()

            personResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
    }

    private fun spesifikasjonerForPerson(person: Person, behandlingKategori: BehandlingKategori): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.valueOfType(behandlingKategori))

        return relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
    }

    private fun vilkårResultater(personResultat: PersonResultat,
                                 barn: List<Person>,
                                 evalueringer: List<Evaluering>): Set<VilkårResultat> {
        return evalueringer.map { child ->
            val tom: LocalDate? = if (Vilkår.valueOf(child.identifikator) == Vilkår.UNDER_18_ÅR) barn.first().fødselsdato.plusYears(18).minusMonths(1) else null

            VilkårResultat(personResultat = personResultat,
                           resultat = child.resultat,
                           vilkårType = Vilkår.valueOf(child.identifikator),
                           periodeFom = barn.first().fødselsdato.plusMonths(1),
                           periodeTom = tom,
                           begrunnelse = "")
        }.toSet()
    }
}