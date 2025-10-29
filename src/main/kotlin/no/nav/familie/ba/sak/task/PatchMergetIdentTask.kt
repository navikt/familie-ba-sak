package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchMergetIdentTask.TASK_STEP_TYPE,
    beskrivelse = "Patcher ident for identer som er merget",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PatchMergetIdentTask(
    private val personidentService: PersonidentService,
    private val aktørMergeLoggRepository: AktørMergeLoggRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val pdlIdentRestKlient: PdlIdentRestKlient,
    private val aktørIdRepository: AktørIdRepository,
    private val personidentRepository: PersonidentRepository,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = objectMapper.readValue(task.payload, PatchMergetIdentDto::class.java)
        secureLogger.info("Patcher ident på fagsak $dto")

        if (dto.gammelIdent == dto.nyIdent) {
            throw IllegalArgumentException("ident som skal patches er lik ident som det skal patches til")
        }

        val aktørerForIdentSomSkalPatches =
            (
                persongrunnlagService
                    .hentSøkerOgBarnPåFagsak(fagsakId = dto.fagsakId)
                    ?.filter { it.type in listOf(PersonType.BARN, PersonType.SØKER) && it.aktør.aktivFødselsnummer() == dto.gammelIdent.ident }
                    ?.map { it.aktør.aktørId } ?: emptyList()
            ).toSet()

        if (aktørerForIdentSomSkalPatches.size > 1) throw Feil("Fant flere aktører for ident som skal patches. fagsak=${dto.fagsakId} aktører=$aktørerForIdentSomSkalPatches")
        val aktørSomSkalPatches = aktørerForIdentSomSkalPatches.firstOrNull() ?: throw Feil("Fant ikke ident som skal patches på fagsak=${dto.fagsakId} aktører=$aktørerForIdentSomSkalPatches")

        val identer = pdlIdentRestKlient.hentIdenter(personIdent = dto.nyIdent.ident, historikk = true)
        if (dto.skalSjekkeAtGammelIdentErHistoriskAvNyIdent) {
            if (identer.none { it.ident == dto.gammelIdent.ident && it.historisk }) {
                throw Feil("Ident som skal patches finnes ikke som historisk ident av ny ident")
            }
        }

        val personidentNyttFødselsnummer = personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident)
        if (personidentNyttFødselsnummer != null) throw Feil("Fant allerede en personident for nytt fødselsnummer")

        // Denne patcher med å bruke on cascade update på aktørid
        aktørIdRepository.patchAktørMedNyAktørId(
            gammelAktørId = aktørSomSkalPatches,
            nyAktørId = identer.hentAktivAktørId(),
        )

        // Etter at alle fk_aktoer_id har blitt oppgradert alle steder, så vil personident ha det gamle fødselsnummeret.
        // Ved å kalle hentOgLagre, så vil den gamle personidenten-raden bli deaktivert og ny med riktig fødselsnummer
        // vil bli oppdatert
        val nyAktør = personidentService.hentOgLagreAktør(ident = dto.nyIdent.ident, lagre = true)

        aktørMergeLoggRepository.save(
            AktørMergeLogg(
                fagsakId = dto.fagsakId,
                historiskAktørId = aktørSomSkalPatches,
                nyAktørId = nyAktør.aktørId,
                mergeTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "PatchMergetIdentTask"
    }
}

data class PatchMergetIdentDto(
    val fagsakId: Long,
    val gammelIdent: PersonIdent,
    val nyIdent: PersonIdent,
    /*
    Sjekker at gammel ident er historisk av ny. Hvis man ønsker å patche med en ident hvor den gamle ikke er
    historisk av ny, så settes denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er
    samme person. Dette kan skje hvis identen ikke er merget av folketrygden.
     */
    val skalSjekkeAtGammelIdentErHistoriskAvNyIdent: Boolean = true,
)
