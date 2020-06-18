package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatPut
import no.nav.familie.ba.sak.common.Feil
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

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
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
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
            personResultat.setVilkårResultater(vilkårResultater(personResultat, barnet, evalueringer))
            personResultat
        }.toSet()

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat, true)
    }

    fun lagInitieltBehandlingResultatFraAnnenBehandling(behandling: Behandling, annenBehandling: Behandling): BehandlingResultat {
        val initiertBehandlingResultat = lagInitieltBehandlingResultat(behandling = behandling)

        val forrigeBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = annenBehandling.id)
                                        ?: throw Feil(message = "Finner ikke behandlingsresultat fra annen behandling.")
        val (oppdatert) = flyttResultaterTilInitielt(aktivtBehandlingResultat = forrigeBehandlingResultat,
                                                             initieltBehandlingResultat = initiertBehandlingResultat)
        return oppdatert
    }

    fun initierVilkårvurderingForBehandling(behandling: Behandling, bekreftEndringerViaFrontend: Boolean): BehandlingResultat {
        val initiertBehandlingResultat = lagInitieltBehandlingResultat(behandling = behandling)
        val aktivBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)

        val annenBehandling = behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        return if (annenBehandling != null && aktivBehandlingResultat == null) {
            val behandlingResultat =
                    lagInitieltBehandlingResultatFraAnnenBehandling(behandling = behandling, annenBehandling = annenBehandling)
            return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat, false)
        } else {
            if (aktivBehandlingResultat != null) {
                val (oppdatert, aktivt) = flyttResultaterTilInitielt(aktivtBehandlingResultat = aktivBehandlingResultat,
                                                                     initieltBehandlingResultat = initiertBehandlingResultat)
                if (aktivt.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                    throw Feil(message = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                               frontendFeilmelding = lagFjernAdvarsel(aktivt.personResultater)
                    )
                }
                return behandlingResultatService.lagreNyOgDeaktiverGammel(oppdatert, false)
            } else {
                behandlingResultatService.lagreInitiert(initiertBehandlingResultat)
            }
        }
    }

    fun lagInitieltBehandlingResultat(behandling: Behandling): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling $behandling")

        val søknadDTO = søknadGrunnlagService.hentAktiv(behandling.id)?.hentSøknadDto()

        val behandlingResultat = BehandlingResultat(behandling = behandling)
        behandlingResultat.personResultater = personopplysningGrunnlag.personer.map { person ->
            val personResultat =
                    lagPersonResultat(person = person, behandlingResultat = behandlingResultat, søknadDTO = søknadDTO)
            personResultat
        }.toSet()

        return behandlingResultat
    }

    private fun lagPersonResultat(person: Person, behandlingResultat: BehandlingResultat, søknadDTO: SøknadDTO?): PersonResultat {
        val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                            personIdent = person.personIdent.ident)

        val sakType = hentSakType(behandlingKategori = behandlingResultat.behandling.kategori, søknadDTO = søknadDTO)

        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, sakType)
        personResultat.setVilkårResultater(relevanteVilkår.flatMap { vilkår ->
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
        }.toSet())
        return personResultat
    }

    private fun spesifikasjonerForPerson(person: Person, behandlingKategori: BehandlingKategori): Spesifikasjon<Fakta> {
        val relevanteVilkår = Vilkår.hentVilkårFor(person.type, SakType.valueOfType(behandlingKategori))

        return relevanteVilkår
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
    }

    private fun vilkårResultater(personResultat: PersonResultat,
                                 barnet: Person,
                                 evalueringer: List<Evaluering>): SortedSet<VilkårResultat> {
        return evalueringer.map { child ->
            val tom: LocalDate? =
                    if (Vilkår.valueOf(child.identifikator) == Vilkår.UNDER_18_ÅR) barnet.fødselsdato.plusYears(18) else null

            VilkårResultat(personResultat = personResultat,
                           resultat = child.resultat,
                           vilkårType = Vilkår.valueOf(child.identifikator),
                           periodeFom = barnet.fødselsdato,
                           periodeTom = tom,
                           begrunnelse = "")
        }.toSortedSet(PersonResultat.comparator)
    }

    @Transactional
    fun endreVilkår(behandlingId: Long,
                    vilkårId: Long,
                    restPersonResultat: RestPersonResultat): List<RestPersonResultat> {
        val behandlingResultat = hentVilkårsvurdering(behandlingId = behandlingId)
                                 ?: throw Feil(message = "Fant ikke aktiv vilkårsvurdering ved endring på vilkår",
                                               frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering")

        val restVilkårResultat = restPersonResultat.vilkårResultater.first()
        val personResultat = behandlingResultat.personResultater.find { it.personIdent == restPersonResultat.personIdent }
                             ?: throw Feil(message = "Fant ikke vilkårsvurdering for person",
                                           frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${restPersonResultat.personIdent}")

        muterPersonResultatPut(personResultat, restVilkårResultat)

        return behandlingResultatService.oppdater(behandlingResultat).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun deleteVilkår(behandlingId: Long, vilkårId: Long, personIdent: String): List<RestPersonResultat> {
        val behandlingResultat = hentVilkårsvurdering(behandlingId = behandlingId)
                                 ?: throw Feil(message = "Fant ikke aktiv vilkårsvurdering ved sletting av vilkår",
                                               frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering")

        val personResultat = behandlingResultat.personResultater.find { it.personIdent == personIdent }
                             ?: throw Feil(message = "Fant ikke vilkårsvurdering for person",
                                           frontendFeilmelding = "Fant ikke vilkårsvurdering for person med ident '${personIdent}")

        muterPersonResultatDelete(personResultat, vilkårId)

        return behandlingResultatService.oppdater(behandlingResultat).personResultater.map { it.tilRestPersonResultat() }
    }

    @Transactional
    fun postVilkår(behandlingId: Long, restNyttVilkår: RestNyttVilkår): List<RestPersonResultat> {
        val behandlingResultat = hentVilkårsvurdering(behandlingId = behandlingId)
                                 ?: throw Feil(message = "Fant ikke aktiv vilkårsvurdering ved opprettelse av vilkår",
                                               frontendFeilmelding = "Fant ikke aktiv vilkårsvurdering")

        val personResultat = behandlingResultat.personResultater.find { it.personIdent == restNyttVilkår.personIdent }
                             ?: throw Feil(message = "Fant ikke vilkårsvurdering for person",
                                           frontendFeilmelding =
                                           "Fant ikke vilkårsvurdering for person med ident '${restNyttVilkår.personIdent}")

        muterPersonResultatPost(personResultat, restNyttVilkår.vilkårType)

        return behandlingResultatService.oppdater(behandlingResultat).personResultater.map { it.tilRestPersonResultat() }
    }
}
