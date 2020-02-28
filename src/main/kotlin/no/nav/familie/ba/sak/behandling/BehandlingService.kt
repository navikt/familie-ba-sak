package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
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
                        private val integrasjonTjeneste: IntegrasjonTjeneste,
                        private val featureToggleService: FeatureToggleService,
                        private val taskRepository: TaskRepository) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Fagsak {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(nyBehandling.ident))
                     ?: throw IllegalStateException("Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktiv(fagsak.id)

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

        val aktivBehandling = hentAktiv(fagsak.id)

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
        lagreNyOgDeaktiverGammelBehandling(behandling)
        return behandling
    }

    private fun lagreSøkerOgBarnIPersonopplysningsgrunnlaget(fødselsnummer: String,
                                                             barnasFødselsnummer: Array<String>,
                                                             behandling: Behandling) {
        val personopplysningGrunnlag =
                personopplysningGrunnlagRepository.save(PersonopplysningGrunnlag(behandlingId = behandling.id))

        val søker = Person(personIdent = behandling.fagsak.personIdent,
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = integrasjonTjeneste.hentPersoninfoFor(fødselsnummer).fødselsdato,
                           aktørId = behandling.fagsak.aktørId
        )

        personopplysningGrunnlag.personer.add(søker)
        lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer, personopplysningGrunnlag)
    }

    private fun lagreBarnPåEksisterendePersonopplysningsgrunnlag(barnasFødselsnummer: Array<String>,
                                                                 personopplysningGrunnlag: PersonopplysningGrunnlag) {

        personopplysningGrunnlag.personer.addAll(leggTilBarnIPersonListe(barnasFødselsnummer, personopplysningGrunnlag))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
    }

    private fun leggTilBarnIPersonListe(barnasFødselsnummer: Array<String>,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag): List<Person> {
        return barnasFødselsnummer.filter { barn ->
            personopplysningGrunnlag.barna.none { eksisterendeBarn -> barn == eksisterendeBarn.personIdent.ident }
        }.map { nyttBarn ->
            Person(personIdent = PersonIdent(nyttBarn),
                   type = PersonType.BARN,
                   personopplysningGrunnlag = personopplysningGrunnlag,
                   fødselsdato = integrasjonTjeneste.hentPersoninfoFor(nyttBarn).fødselsdato,
                   aktørId = hentAktørIdOrNull(nyttBarn)
            )
        }
    }

    fun hentAktiv(fagsakId: Long?): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long?): Behandling? {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentAktiveBehandlingerForLøpendeFagsaker(): List<OppdragId> {
        return fagsakService.hentLøpendeFagsaker()
                .mapNotNull { fagsak -> hentAktiv(fagsak.id) }
                .map { behandling ->
                    OppdragId(
                            hentSøker(behandling)!!.personIdent.ident,
                            behandling.id)
                }
    }

    fun hentBehandlinger(fagsakId: Long?): List<Behandling?> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling) {
        val aktivBehandling = hentAktiv(behandling.fagsak.id)

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingRepository.save(aktivBehandling)
        }

        behandlingRepository.save(behandling)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    fun valider2trinnVedIverksetting(behandling: Behandling, ansvarligSaksbehandler: String) {
        if (behandling.endretAv == ansvarligSaksbehandler) {
            throw IllegalStateException("Samme saksbehandler kan ikke foreslå og iverksette samme vedtak")
        }

        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.GODKJENT)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long?, status: BehandlingStatus) {
        when (val behandling = hent(behandlingId)) {
            null -> throw Exception("Feilet ved oppdatering av status på behandling. Fant ikke behandling med id $behandlingId")
            else -> {
                LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

                behandling.status = status
                behandlingRepository.save(behandling)
            }
        }
    }

    private fun hentSøker(behandling: Behandling): Person? {
        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)!!.personer
                .find { person -> person.type == PersonType.SØKER }
    }

    private fun hentAktørIdOrNull(ident: String): AktørId? {
        return Result.runCatching {
            return integrasjonTjeneste.hentAktørId(ident)
        }.fold(
                onSuccess = { it },
                onFailure = { null }
        )
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
