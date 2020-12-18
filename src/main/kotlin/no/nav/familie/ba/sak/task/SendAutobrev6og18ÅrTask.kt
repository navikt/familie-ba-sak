package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24) //TODO: Vi trenger kanskje ikke denne? Hva er default?
class SendAutobrev6og18ÅrTask(
        private val behandlingService: BehandlingService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)
        val behandling = behandlingService.hentAktivForFagsak(autobrevDTO.fagsakId) ?: error("Fant ikke aktiv behandling")

        // Finne ut om fagsak er løpende -> hvis nei, avslutt uten feil
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) return

        // Finne ut om fagsak har behandling som ikke er fullført -> hvis ja, feile task og logge feil og øke metrikk
        //  hvis tasken forsøker for siste gang -> opprett oppgave for å håndtere videre manuelt
        if (behandling.status != BehandlingStatus.AVSLUTTET) {
            error("Kan ikke opprette ny behandling for fagsak ${behandling.fagsak.id} ettersom den allerede har en åpen behanding.")
        }

        val personerIBehandling = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                                  ?: error("Fant ingen personer på behandling ${behandling.id}")

        // Hvis barn er 18 år og ingen andre barn med annen alder er på fagsaken -> avslutt
        if (autobrevDTO.alder == 18) {
            if (personerIBehandling.personer.filter { person ->
                        person.type == PersonType.BARN && !person.fyllerAntallÅrInneværendeMåned(autobrevDTO.alder)
                    }.isEmpty()) return
        }

        val barnMedOppgittAlder = personerIBehandling.personer.filter { person ->
            person.type == PersonType.BARN && person.fyllerAntallÅrInneværendeMåned(autobrevDTO.alder)
        }

        if (barnMedOppgittAlder.isEmpty()) {
            error("Fant ingen barn som fyller ${autobrevDTO.alder} inneværende måned for behandling ${behandling.id}")
        }

        // Opprett ny behandling (revurdering) med årsak "Omregning". Vilkårsvurdering skal være uforandret. Fullfør
        // behandling uten manuell to-trinnskontroll og oversendelse til økonomi.

        val nybehandling = NyBehandling(søkersIdent = behandling.fagsak.hentAktivIdent().ident,
                                        behandlingType = BehandlingType.REVURDERING,
                                        kategori = behandling.kategori,
                                        underkategori = behandling.underkategori,
                                        behandlingÅrsak = BehandlingÅrsak.OMREGNING,
                                        skalBehandlesAutomatisk = true
        )

        // Oppretter behandling, men her gjenstår å få kopiert persongrunnlag og vilkårsvurdering og
        // fullført behandling. Bør kanskje legges til stegservice?
        val revurdering = behandlingService.opprettBehandling(nybehandling)

        // Send brev, journalfør og skriv metrikk.

        LOG.info("SendAutobrev6og18ÅrTask for fagsak ${autobrevDTO.fagsakId}")
    }

    companion object {

        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        val LOG: Logger = LoggerFactory.getLogger(SendAutobrev6og18ÅrTask::class.java)
    }
}

fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
    return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
           this.fødselsdato.isBefore(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
}

