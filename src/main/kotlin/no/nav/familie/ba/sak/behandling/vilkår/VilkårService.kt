package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
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

    fun hentVilkårsvurdering(behandlingId: Long): BehandlingResultat? = behandlingResultatService.hentAktivForBehandling(behandlingId = behandlingId)

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

    fun initierVilkårvurderingForBehandling(behandling: Behandling,
                                            bekreftEndringerViaFrontend: Boolean,
                                            forrigeBehandling: Behandling? = null): BehandlingResultat {

        if (behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
            val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                           ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")
            val barna = personopplysningGrunnlag.personer.filter { person -> person.type === PersonType.BARN }
            if (barna.isEmpty()) {
                throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
            }
        }

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
            val evalueringerForVilkår = spesifikasjonererForPerson.map { it.evaluer(fakta) }

            personResultat.setVilkårResultater(vilkårResultater(personResultat, person, fakta, evalueringerForVilkår))

            personResultat
        }.toSet()
    }

    private fun spesifikasjonerForPerson(person: Person): List<Spesifikasjon<Fakta>> = Vilkår.hentVilkårFor(person.type).map { vilkår -> vilkår.spesifikasjon}

    private fun vilkårResultater(personResultat: PersonResultat,
                                 person: Person,
                                 fakta: Fakta,
                                 evalueringer: List<Evaluering>): SortedSet<VilkårResultat> {

        return evalueringer.map { child ->
            val fom =
                    if (person.type === PersonType.BARN)
                        person.fødselsdato
                    else LocalDate.now()

            val vilkår =
                    if (child.identifikator == "" && child.children.isNotEmpty())
                        Vilkår.valueOf(child.children.first().identifikator.split(":")[0])
                    else
                        Vilkår.valueOf(child.identifikator.split(":")[0])

            val tom: LocalDate? =
                    if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18) else null

            if (førstegangskjøringAvVilkårsvurdering(personResultat)) {
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
                           periodeFom = fom,
                           periodeTom = tom,
                           begrunnelse = begrunnelse,
                           behandlingId = personResultat.behandlingResultat.behandling.id,
                           regelInput = fakta.toJson(),
                           regelOutput = child.toJson()
            )
        }.toSortedSet(PersonResultat.comparator)
    }

    private fun førstegangskjøringAvVilkårsvurdering(personResultat: PersonResultat): Boolean {
        return behandlingResultatService
                .hentAktivForBehandling(behandlingId = personResultat.behandlingResultat.behandling.id) == null
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
