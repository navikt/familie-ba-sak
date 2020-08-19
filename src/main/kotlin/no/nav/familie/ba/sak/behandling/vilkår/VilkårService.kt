package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatDelete
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatPost
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.muterPersonResultatPut
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class VilkårService(
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val vilkårsvurderingMetrics: VilkårsvurderingMetrics
) {

    fun hentVilkårsdato(behandling: Behandling): LocalDate? {
        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)
                                 ?: error("Finner ikke behandlingsresultat på behandling ${behandling.id}")

        val periodeResultater = behandlingResultat.periodeResultater(brukMåned = false)
        return periodeResultater.first {
            it.allePåkrevdeVilkårErOppfylt(PersonType.SØKER) &&
            it.allePåkrevdeVilkårErOppfylt(PersonType.BARN)
        }.periodeFom
    }

    fun hentVilkårsvurdering(behandlingId: Long): BehandlingResultat? = behandlingResultatService.hentAktivForBehandling(
            behandlingId = behandlingId)

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

    fun vurderVilkårForFødselshendelse(behandlingId: Long): BehandlingResultat {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")
        val barna = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
        if (barna.size != 1) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde ett barn, men inneholder ${barna.size}")
        }

        val behandlingResultat = BehandlingResultat(
                behandling = behandlingService.hent(behandlingId),
                aktiv = true)

        lagOgKjørAutomatiskVilkårsvurdering(behandlingResultat = behandlingResultat)

        return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat, loggHendelse = true)
    }

    fun initierVilkårvurderingForBehandling(behandling: Behandling,
                                            bekreftEndringerViaFrontend: Boolean,
                                            forrigeBehandling: Behandling? = null): BehandlingResultat {
        val initieltBehandlingResultat = genererInitieltBehandlingResultat(behandling = behandling)
        val aktivBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandling.id)

        return if (forrigeBehandling != null && aktivBehandlingResultat == null) {
            val behandlingResultat =
                    genererInitieltBehandlingResultatFraAnnenBehandling(behandling = behandling,
                                                                        annenBehandling = forrigeBehandling)
            return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat,
                                                                      loggHendelse = false)
        } else {
            if (aktivBehandlingResultat != null) {
                val (initieltSomErOppdatert, aktivtSomErRedusert) = flyttResultaterTilInitielt(
                        initieltBehandlingResultat = initieltBehandlingResultat,
                        aktivtBehandlingResultat = aktivBehandlingResultat
                )

                if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
                    throw Feil(message = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                               frontendFeilmelding = lagFjernAdvarsel(aktivtSomErRedusert.personResultater)
                    )
                }
                return behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = initieltSomErOppdatert,
                                                                          loggHendelse = false)
            } else {
                behandlingResultatService.lagreInitielt(initieltBehandlingResultat)
            }
        }
    }

    fun genererInitieltBehandlingResultatFraAnnenBehandling(behandling: Behandling,
                                                            annenBehandling: Behandling): BehandlingResultat {
        val initieltBehandlingResultat = genererInitieltBehandlingResultat(behandling = behandling)

        val forrigeBehandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = annenBehandling.id)
                                        ?: throw Feil(message = "Finner ikke behandlingsresultat fra annen behandling.")
        val (oppdatert) = flyttResultaterTilInitielt(aktivtBehandlingResultat = forrigeBehandlingResultat,
                                                     initieltBehandlingResultat = initieltBehandlingResultat)
        return oppdatert
    }

    private fun genererInitieltBehandlingResultat(behandling: Behandling): BehandlingResultat {
        val behandlingResultat = BehandlingResultat(behandling = behandling)
        lagOgKjørAutomatiskVilkårsvurdering(behandlingResultat = behandlingResultat)

        return behandlingResultat
    }

    private fun lagOgKjørAutomatiskVilkårsvurdering(behandlingResultat: BehandlingResultat) {
        val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingResultat.behandling.id)
                ?: throw Feil(message = "Fant ikke personopplysninggrunnlag for behandling ${behandlingResultat.behandling.id}")



        behandlingResultat.personResultater = personopplysningGrunnlag.personer.map { person ->
            val personResultat = PersonResultat(behandlingResultat = behandlingResultat,
                                                personIdent = person.personIdent.ident)

            val spesifikasjonererForPerson = spesifikasjonerForPerson(person)
            val fakta = Fakta(personForVurdering = person, behandlingOpprinnelse = behandlingResultat.behandling.opprinnelse)
            val evalueringerForVilkår = spesifikasjonererForPerson.map {
                val vilkår =
                        if (it.identifikator == "" && it.children.isNotEmpty())
                            Vilkår.valueOf(it.children.first().identifikator.split(":")[0])
                        else
                            Vilkår.valueOf(it.identifikator.split(":")[0])

                val minLocalDate =
                        if (person.type == PersonType.BARN)
                            person.fødselsdato
                        else
                            personopplysningGrunnlag.barna.minBy { barn -> barn.fødselsdato }!!.fødselsdato

                val perioder = vilkår.genererPerioder(fakta, minLocalDate)

                perioder.map { periode ->
                    val evaluering = it.evaluer(fakta.copy(periode = periode))

                    Pair(periode, evaluering)
                }
            }

            personResultat.setVilkårResultater(vilkårResultater(personResultat, person, fakta, evalueringerForVilkår))

            personResultat
        }.toSet()
    }

    private fun spesifikasjonerForPerson(person: Person): List<Spesifikasjon<Fakta>> = Vilkår.hentVilkårFor(person.type)
            .map { vilkår -> vilkår.spesifikasjon }

    private fun vilkårResultater(personResultat: PersonResultat,
                                 person: Person,
                                 fakta: Fakta,
                                 evalueringer: List<List<Pair<Periode, Evaluering>>>): SortedSet<VilkårResultat> {

        val aktivBehandlingResultat =
                behandlingResultatService.hentAktivForBehandling(behandlingId = personResultat.behandlingResultat.behandling.id)
        val kjørMetrikker = aktivBehandlingResultat == null

        return evalueringer.map { listeAvPeriodeOgEvaluering ->
            listeAvPeriodeOgEvaluering.map { pair ->
                val periode = pair.first
                val child = pair.second

                val vilkår =
                        if (child.identifikator == "" && child.children.isNotEmpty())
                            Vilkår.valueOf(child.children.first().identifikator.split(":")[0])
                        else
                            Vilkår.valueOf(child.identifikator.split(":")[0])

                if (kjørMetrikker) {
                    vilkårsvurderingMetrics.økTellereForEvaluering(evaluering = child,
                                                                   personType = person.type,
                                                                   behandlingOpprinnelse = personResultat.behandlingResultat.behandling.opprinnelse)
                }

                var begrunnelse = "Vurdert og satt automatisk"

                if (child.resultat == Resultat.NEI || child.resultat == Resultat.KANSKJE) {
                    if (child.children.isNotEmpty())
                        child.children.forEach {
                            if (it.begrunnelse.isNotBlank()) {
                                when (it.resultat) {
                                    Resultat.NEI ->
                                        begrunnelse = "$begrunnelse\n\t- nei: ${it.begrunnelse}"
                                    Resultat.KANSKJE ->
                                        begrunnelse = "$begrunnelse\n\t- kanskje: ${it.begrunnelse}"
                                }
                            }
                        }
                    else
                        begrunnelse = "$begrunnelse\n\t- ${child.begrunnelse}"
                }

                VilkårResultat(personResultat = personResultat,
                               resultat = child.resultat,
                               vilkårType = vilkår,
                               periodeFom = if (periode.fom == TIDENES_MORGEN) null else periode.fom,
                               periodeTom = if (periode.tom == TIDENES_ENDE) null else periode.tom,
                               begrunnelse = begrunnelse,
                               behandlingId = personResultat.behandlingResultat.behandling.id,
                               regelInput = fakta.toJson(),
                               regelOutput = child.toJson()
                )
            }
        }.flatten().toSortedSet(PersonResultat.comparator)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
