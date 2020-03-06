package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.mottak.NyBehandling
import no.nav.familie.ba.sak.mottak.NyBehandlingHendelse
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.OpprettBehandleSakOppgaveForNyBehandlingTask
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                        private val fagsakService: FagsakService,
                        private val integrasjonClient: IntegrasjonClient,
                        private val featureToggleService: FeatureToggleService,
                        private val taskRepository: TaskRepository) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Fagsak {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(nyBehandling.ident))
                     ?: error("Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.FERDIGSTILT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak,
                                                         nyBehandling.journalpostID,
                                                         nyBehandling.behandlingType,
                                                         nyBehandling.kategori,
                                                         nyBehandling.underkategori)
            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling.ident, nyBehandling.barnasIdenter, behandling)
        } else {
            throw IllegalStateException("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }

        return fagsak
    }

    @Transactional
    fun opprettEllerOppdaterBehandlingFraHendelse(nyBehandling: NyBehandlingHendelse): Fagsak {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandling.fødselsnummer)

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.FERDIGSTILT) {
            val behandling = opprettNyBehandlingPåFagsak(fagsak,
                                                         null,
                                                         BehandlingType.FØRSTEGANGSBEHANDLING,
                                                         BehandlingKategori.NASJONAL,
                                                         BehandlingUnderkategori.ORDINÆR)

            lagreSøkerOgBarnIPersonopplysningsgrunnlaget(nyBehandling.fødselsnummer, nyBehandling.barnasFødselsnummer, behandling)
            if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
                val nyTask = Task.nyTask(OpprettBehandleSakOppgaveForNyBehandlingTask.TASK_STEP_TYPE, behandling.id.toString())
                taskRepository.save(nyTask)
            } else {
                LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
            }
        } else if (aktivBehandling.status == BehandlingStatus.OPPRETTET || aktivBehandling.status == BehandlingStatus.UNDER_BEHANDLING) {
            val grunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(aktivBehandling.id)
            lagreBarnPåEksisterendePersonopplysningsgrunnlag(nyBehandling.barnasFødselsnummer, grunnlag!!)
            aktivBehandling.status = BehandlingStatus.OPPRETTET
            behandlingRepository.save(aktivBehandling)
        } else {
            throw Exception("Kan ikke lagre ny behandling. Fagsaken er ferdig behandlet og sendt til iverksetting.")
        }

        return fagsak
    }

    fun settVilkårsvurdering(behandling: Behandling, resultat: BehandlingResultat, begrunnelse: String): Behandling {
        behandling.begrunnelse = begrunnelse
        behandling.resultat = resultat
        return lagre(behandling)
    }

    fun opprettNyBehandlingPåFagsak(fagsak: Fagsak,
                                    journalpostID: String?,
                                    behandlingType: BehandlingType,
                                    kategori: BehandlingKategori,
                                    underkategori: BehandlingUnderkategori): Behandling {
        val behandling =
                Behandling(fagsak = fagsak,
                           journalpostID = journalpostID,
                           type = behandlingType,
                           kategori = kategori,
                           underkategori = underkategori)
        return lagreNyOgDeaktiverGammelBehandling(behandling)
    }

    private fun lagreSøkerOgBarnIPersonopplysningsgrunnlaget(fødselsnummer: String,
                                                             barnasFødselsnummer: List<String>,
                                                             behandling: Behandling) {
        val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.save(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val søker = Person(personIdent = behandling.fagsak.personIdent,
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = integrasjonClient.hentPersoninfoFor(fødselsnummer).fødselsdato,
                           aktørId = behandling.fagsak.aktørId
        )

        personopplysningGrunnlag.personer.add(søker)
        lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer, personopplysningGrunnlag)
    }

    private fun lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer: List<String>,
                                                                 personopplysningGrunnlag: PersonopplysningGrunnlag) {

        personopplysningGrunnlag.personer.addAll(leggTilBarnIPersonListe(barnasFødselsnummer, personopplysningGrunnlag))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun leggTilBarnIPersonListe(barnasFødselsnummer: List<String>,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.filter { barn ->
            personopplysningGrunnlag.barna.none { eksisterendeBarn -> barn == eksisterendeBarn.personIdent.ident }
        }.map { nyttBarn ->
            Person(personIdent = PersonIdent(nyttBarn),
                   type = PersonType.BARN,
                   personopplysningGrunnlag = personopplysningGrunnlag,
                   fødselsdato = integrasjonClient.hentPersoninfoFor(nyttBarn).fødselsdato,
                   aktørId = hentAktørIdOrNull(nyttBarn)
            )
        }
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentAktiveBehandlingerForLøpendeFagsaker(): List<OppdragId> {
        return fagsakService.hentLøpendeFagsaker()
                .mapNotNull { fagsak -> hentAktivForFagsak(fagsak.id) }
                .map { behandling ->
                    OppdragId(
                            hentSøker(behandling)!!.personIdent.ident,
                            behandling.id)
                }
    }

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagre(behandling: Behandling): Behandling {
        return behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus) {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        behandlingRepository.save(behandling)
    }

    private fun hentSøker(behandling: Behandling): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
                .find { person -> person.type == PersonType.SØKER }
    }

    private fun hentAktørIdOrNull(ident: String): AktørId? {
        return Result.runCatching {
            return integrasjonClient.hentAktørId(ident)
        }.fold(
                onSuccess = { it },
                onFailure = { null }
        )
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
