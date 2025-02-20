package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchIdentFagsakUtenBehandling.TASK_STEP_TYPE,
    beskrivelse = "Patcher ident på fagsak uten behandling hvis identen er utdatert",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PatchIdentFagsakUtenBehandling(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val pdlIdentRestClient: PdlIdentRestClient,
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val personidentService: PersonidentService,
    private val aktørMergeLoggRepository: AktørMergeLoggRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakId = objectMapper.readValue(task.payload, Long::class.java)

        // Valider at fagsak ikke har behandlinger
        val behandlingerPåFagsak = behandlingRepository.finnBehandlinger(fagsakId)
        if (behandlingerPåFagsak.isNotEmpty()) {
            throw IllegalArgumentException("Fagsak $fagsakId har behandlinger og burde patches på en annen måte")
        }

        // Valider at identen på fagsaken er utdatert
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw IllegalArgumentException("Fagsak $fagsakId eksisterer ikke")
        val aktivPersonIdent = fagsak.aktør.aktivFødselsnummer()

        val identInformasjonFraPdl = pdlIdentRestClient.hentIdenter(personIdent = aktivPersonIdent, historikk = true)
        val erIdentPåFagsakAjourMedPdl =
            identInformasjonFraPdl.none { identFraPdl ->
                identFraPdl.historisk && identFraPdl.ident == aktivPersonIdent
            }
        if (erIdentPåFagsakAjourMedPdl) {
            return
        }

        secureLogger.info("Patcher aktør med id ${fagsak.aktør.aktørId} på fagsak $fagsakId")
        val nyPersonIdent = identInformasjonFraPdl.hentAktivFødselsnummer()

        val personidentNyttFødselsnummer = personidentRepository.findByFødselsnummerOrNull(nyPersonIdent)
        if (personidentNyttFødselsnummer != null) error("Fant allerede en personident for nytt fødselsnummer")

        // Patch hvis utdatert - trekk ut funksjonalitet fra PatchMergetIdentTask
        // Denne patcher med å bruke on cascade update på aktørid
        val eksisterendeAktørPåFagsak = fagsak.aktør.aktørId
        aktørIdRepository.patchAktørMedNyAktørId(
            gammelAktørId = eksisterendeAktørPåFagsak,
            nyAktørId = identInformasjonFraPdl.hentAktivAktørId(),
        )

        // Etter at alle fk_aktoer_id har blitt oppgradert alle steder, så vil personident ha det gamle fødselsnummeret.
        // Ved å kalle hentOgLagre, så vil den gamle personidenten-raden bli deaktivert og ny med riktig fødselsnummer
        // vil bli oppdatert
        val nyAktør = personidentService.hentOgLagreAktør(ident = nyPersonIdent, lagre = true)

        aktørMergeLoggRepository.save(
            AktørMergeLogg(
                fagsakId = fagsakId,
                historiskAktørId = eksisterendeAktørPåFagsak,
                nyAktørId = nyAktør.aktørId,
                mergeTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "PatchIdentFagsakUtenBehandling"
    }
}
