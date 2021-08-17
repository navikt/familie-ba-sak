package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(private val integrasjonClient: IntegrasjonClient,
                     private val personopplysningerService: PersonopplysningerService,
                     private val behandlingRepository: BehandlingRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository) {

    private val antallOppgaveTyper: MutableMap<Oppgavetype, Counter> = mutableMapOf()

    fun opprettOppgave(behandlingId: Long,
                       oppgavetype: Oppgavetype,
                       fristForFerdigstillelse: LocalDate,
                       tilordnetNavIdent: String? = null,
                       beskrivelse: String? = null): String {

        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val fagsakId = behandling.fagsak.id

        val eksisterendeOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)

        return if (eksisterendeOppgave != null
                   && oppgavetype != Oppgavetype.Journalføring) {
            logger.warn("Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt ved opprettelse av ny oppgave ${eksisterendeOppgave}. " +
                        "Vi oppretter ikke ny oppgave, men gjenbruker eksisterende.")

            eksisterendeOppgave.gsakId
        } else {
            val arbeidsfordelingsenhet = arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

            if (arbeidsfordelingsenhet == null) {
                logger.warn("Fant ikke behandlende enhet på behandling ${behandling.id} ved opprettelse av $oppgavetype-oppgave.")
            }

            val aktorId = personopplysningerService.hentAktivAktørId(Ident(behandling.fagsak.hentAktivIdent().ident)).id
            val opprettOppgave = OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = aktorId, gruppe = IdentGruppe.AKTOERID),
                    saksId = fagsakId.toString(),
                    tema = Tema.BAR,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristForFerdigstillelse,
                    beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                    enhetsnummer = arbeidsfordelingsenhet?.behandlendeEnhetId,
                    behandlingstema = Behandlingstema.OrdinærBarnetrygd.value,
                    tilordnetRessurs = tilordnetNavIdent
            )
            val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave)


            val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
            oppgaveRepository.save(oppgave)

            økTellerForAntallOppgaveTyper(oppgavetype)

            opprettetOppgaveId
        }
    }

    private fun økTellerForAntallOppgaveTyper(oppgavetype: Oppgavetype) {
        if (antallOppgaveTyper[oppgavetype] == null) {
            antallOppgaveTyper[oppgavetype] = Metrics.counter("oppgave.opprettet", "type", oppgavetype.name)
        }

        antallOppgaveTyper[oppgavetype]?.increment()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        return integrasjonClient.patchOppgave(patchOppgave)
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String, overstyrFordeling: Boolean = false): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)
            if (oppgave.tilordnetRessurs != null) {
                throw FunksjonellFeil(melding = "Oppgaven er allerede fordelt",
                                      frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}")
            }
        }

        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler)
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonClient.fordelOppgave(oppgaveId, null)
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgaveSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): DbOppgave? {
        return oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)
    }

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> {
        return oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave {
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun ferdigstillOppgave(behandlingId: Long, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype,
                                                                                         behandlingRepository.finnBehandling(
                                                                                                 behandlingId))
                      ?: error("Finner ikke oppgave for behandling $behandlingId")
        integrasjonClient.ferdigstillOppgave(oppgave.gsakId.toLong())

        oppgave.erFerdigstilt = true
        oppgaveRepository.save(oppgave)
    }

    fun lagOppgaveTekst(fagsakId: Long, beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
               "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "https://barnetrygd.nais.adeo.no/fagsak/${fagsakId}"
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return integrasjonClient.hentOppgaver(finnOppgaveRequest)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }
}