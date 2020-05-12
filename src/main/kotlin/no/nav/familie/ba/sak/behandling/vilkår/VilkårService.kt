package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårDiff.lagFjernAdvarsel
import no.nav.familie.ba.sak.behandling.vilkår.VilkårDiff.oppdaterteBehandlingsresultater
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val søknadGrunnlagService: SøknadGrunnlagService
) {

    fun hentVilkårsdato(behandling: Behandling): LocalDate? {
        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)
                                 ?: error("Finner ikke behandlingsresultat på behandling ${behandling.id}")

        val periodeResultater = behandlingResultat.periodeResultater(brukMåned = false)
        return periodeResultater.first {
            it.allePåkrevdeVilkårErOppfylt(PersonType.SØKER,
                                           SakType.valueOfType(behandling.kategori)) &&
            it.allePåkrevdeVilkårErOppfylt(PersonType.BARN,
                                           SakType.valueOfType(
                                                   behandling.kategori))
        }.periodeFom
    }

    fun hentVilkårsvurdering(behandlingId: Long): BehandlingResultat? {
        return behandlingResultatService.hentAktivForBehandling(behandlingId = behandlingId)
    }

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val behandling = behandlingService.hent(behandlingId)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barna = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barna.size != 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde ett barn, men inneholder ${barna.size}")
        }

        val barnet = barna.first()

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
            personResultat.vilkårResultater = vilkårResultater(personResultat, barnet, evalueringer)
            personResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat, true)
    }

    fun initierVilkårvurderingForBehandling(behandlingId: Long): BehandlingResultat {
        val initiertBehandlingResultat = initierMinimaltBehandlingResultatForBehandling(behandlingId)
        val aktivBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId)
        return if (aktivBehandlingResultat != null) {
            val (oppdatert, gammel) = oppdaterteBehandlingsresultater(behandling = behandlingService.hent(behandlingId),
                                                                      aktivtResultat = aktivBehandlingResultat,
                                                                      initiertResultat = initiertBehandlingResultat)

            if (gammel.personResultater.isNotEmpty()) {
                error(lagFjernAdvarsel(gammel.personResultater))
            }

            return behandlingResultatService.lagreNyOgDeaktiverGammel(oppdatert, false)
        } else {
            behandlingResultatService.lagreInitiert(initiertBehandlingResultat)
        }
    }

    fun initierMinimaltBehandlingResultatForBehandling(behandlingId: Long): BehandlingResultat { // TODO: Bedre navn
        val behandling = behandlingService.hent(behandlingId)

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

        val søknadDTO = søknadGrunnlagService.hentAktiv(behandling.id)?.hentSøknadDto()

        val behandlingResultat = BehandlingResultat(behandling = behandlingService.hent(behandlingId),
                                                    aktiv = true)
        behandlingResultat.personResultater = personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                                personIdent = person.personIdent.ident)

            val sakType = hentSakType(behandlingKategori = behandling.kategori, søknadDTO = søknadDTO)

            val relevanteVilkår = Vilkår.hentVilkårFor(person.type, sakType)
            personResultat.vilkårResultater = relevanteVilkår.flatMap { vilkår ->
                val vilkårListe = mutableListOf<VilkårResultat>()
                if (vilkår == Vilkår.UNDER_18_ÅR) {
                    vilkårListe.add(VilkårResultat(personResultat = personResultat,
                                                   resultat = Resultat.JA,
                                                   vilkårType = vilkår,
                                                   periodeFom = person.fødselsdato,
                                                   periodeTom = person.fødselsdato.plusYears(18),
                                                   begrunnelse = "Vurdert og satt automatisk"))
                } else {
                    vilkårListe.add(VilkårResultat(personResultat = personResultat,
                                                   resultat = Resultat.KANSKJE,
                                                   vilkårType = vilkår,
                                                   begrunnelse = ""))
                }
                vilkårListe
            }.toSet()
            personResultat
        }.toSet()
        return behandlingResultat
    }

    fun lagBehandlingResultatFraRestPersonResultater(personResultater: List<RestPersonResultat>,
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

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat, true)
    }

    private fun spesifikasjonerForPerson(person: Person, behandlingKategori: BehandlingKategori): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.valueOfType(behandlingKategori))

        return relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
    }

    private fun vilkårResultater(personResultat: PersonResultat,
                                 barnet: Person,
                                 evalueringer: List<Evaluering>): Set<VilkårResultat> {
        return evalueringer.map { child ->
            val tom: LocalDate? =
                    if (Vilkår.valueOf(child.identifikator) == Vilkår.UNDER_18_ÅR) barnet.fødselsdato.plusYears(18) else null

            VilkårResultat(personResultat = personResultat,
                           resultat = child.resultat,
                           vilkårType = Vilkår.valueOf(child.identifikator),
                           periodeFom = barnet.fødselsdato,
                           periodeTom = tom,
                           begrunnelse = "")
        }.toSet()
    }
}
