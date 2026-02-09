package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.person.pdl.aktor.v2.Type
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchMergetAktørTask.TASK_STEP_TYPE,
    beskrivelse = "Patcher ident for identer som er merget",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PatchMergetAktørTask(
    private val aktørMergeLoggRepository: AktørMergeLoggRepository,
    private val pdlIdentRestKlient: PdlIdentRestKlient,
    private val aktørIdRepository: AktørIdRepository,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = objectMapper.readValue(task.payload, PatchMergetAktørDto::class.java)
        secureLogger.info("Patcher ident på fagsak $dto")

        if (dto.gammelAktørId == dto.nyAktørId) {
            throw IllegalArgumentException("id som skal patches er lik id som det skal patches til")
        }

        val identer =
            pdlIdentRestKlient
                .hentIdenter(personIdent = dto.nyAktørId, historikk = true)
                .filter { it.gruppe == Type.AKTORID.name }

        if (dto.skalSjekkeAtGammelAktørIdErHistoriskAvNyAktørId) {
            if (identer.none { it.ident == dto.gammelAktørId && it.historisk }) {
                throw Feil("AktørId som skal patches finnes ikke som historisk ident av ny ident")
            }
        }

        validerAtAktørIkkeFinnesFraFør(dto)

        // Denne patcher med å bruke on cascade update på aktørid
        aktørIdRepository.patchAktørMedNyAktørId(
            gammelAktørId = dto.gammelAktørId,
            nyAktørId = dto.nyAktørId,
        )

        aktørMergeLoggRepository.save(
            AktørMergeLogg(
                fagsakId = dto.fagsakId,
                historiskAktørId = dto.gammelAktørId,
                nyAktørId = dto.nyAktørId,
                mergeTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    private fun validerAtAktørIkkeFinnesFraFør(dto: PatchMergetAktørDto) {
        val aktørIdIDb = aktørIdRepository.findByAktørIdOrNull(dto.nyAktørId)
        if (aktørIdIDb != null) throw Feil("Fant allerede en aktør med nytt aktørId")
    }

    companion object {
        const val TASK_STEP_TYPE = "PatchMergetAktoerTask"
    }
}

data class PatchMergetAktørDto(
    val fagsakId: Long,
    val gammelAktørId: String,
    val nyAktørId: String,
    /*
    Sjekker at gammel ident er historisk av ny. Hvis man ønsker å patche med en ident hvor den gamle ikke er
    historisk av ny, så settes denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er
    samme person. Dette kan skje hvis identen ikke er merget av folketrygden.
     */
    val skalSjekkeAtGammelAktørIdErHistoriskAvNyAktørId: Boolean = true,
)
