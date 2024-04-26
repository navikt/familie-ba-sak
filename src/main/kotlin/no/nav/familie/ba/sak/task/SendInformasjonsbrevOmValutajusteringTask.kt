package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.leggTilEnhet
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendInformasjonsbrevOmValutajusteringTask.TASK_STEP_TYPE,
    beskrivelse = "Send informasjonsbrev om valutajustering",
    maxAntallFeil = 1,
)
class SendInformasjonsbrevOmValutajusteringTask(
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val dokumentService: DokumentService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val søker = persongrunnlagService.hentSøker(behandlingId = task.payload.toLong())

        val manueltBrevRequest =
            ManueltBrevRequest(
                brevmal = Brevmal.INFORMASJONSBREV_OM_VALUTAJUSTERING,
                mottakerIdent = søker.aktør.aktivFødselsnummer(),
                mottakerNavn = søker.navn,
            ).leggTilEnhet(arbeidsfordelingService)

        dokumentService.sendManueltBrev(
            manueltBrevRequest = manueltBrevRequest,
            fagsakId = (task.metadata["fagsakId"] as String).toLong(),
        )
    }

    companion object {
        fun lagTask(eøsBehandlingMedSekundærland: Behandling) =
            Task(
                type = TASK_STEP_TYPE,
                payload = eøsBehandlingMedSekundærland.id.toString(),
                mapOf("fagsakId" to eøsBehandlingMedSekundærland.fagsak.id.toString()).toProperties(),
            )

        const val TASK_STEP_TYPE = "sendInformasjonsbrevOmValutajustering"
    }
}
