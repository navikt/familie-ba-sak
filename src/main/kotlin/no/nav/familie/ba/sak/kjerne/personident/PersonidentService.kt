package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val personopplysningerService: PersonopplysningerService,
    private val taskRepository: TaskRepositoryWrapper,
) {

    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = personopplysningerService.hentIdenter(nyIdent.ident, false)
        val aktørId = filtrerAktørId(identerFraPdl)

        val aktør = aktørIdRepository.findByIdOrNull(aktørId)

        return if (aktør?.harIdent(fødselsnummer = nyIdent.ident) == false) {
            logger.info("Legger til ny ident")
            secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
            opprettPersonIdent(aktør, nyIdent.ident)
        } else aktør
    }

    @Transactional
    fun opprettTaskForIdentHendelse(nyIdent: PersonIdent) {
        logger.info("Oppretter task for senere håndterering av ny ident")
        secureLogger.info("Oppretter task for senere håndterering av ny ident ${nyIdent.ident}")
        taskRepository.save(
            Task(
                type = IdentHendelseTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(nyIdent)
            ).medTriggerTid(
                triggerTid = LocalDateTime.now().plusMinutes(1) // TODO: Settes med liten forsinkelse for test
            )
        )
    }

    fun hentOgLagreAktørId(fødselsnummer: String): Aktør =
        personidentRepository.findByIdOrNull(fødselsnummer)?.let { it.aktør }
            ?: kotlin.run {
                val identerFraPdl = personopplysningerService.hentIdenter(fødselsnummer, false)
                val aktørIdStr = filtrerAktørId(identerFraPdl)
                val fødselsnummerAktiv = filtrerAktivtFødselsnummer(identerFraPdl)

                return aktørIdRepository.findByIdOrNull(aktørIdStr)?.let { opprettPersonIdent(it, fødselsnummerAktiv) }
                    ?: opprettAktørIdOgPersonident(aktørIdStr, fødselsnummerAktiv)
            }

    fun hentOgLagreAktørIder(barnasFødselsnummer: List<String>): List<Aktør> {
        return barnasFødselsnummer.map { hentOgLagreAktørId(it) }
    }

    private fun opprettAktørIdOgPersonident(aktørIdStr: String, fødselsnummer: String): Aktør =
        aktørIdRepository.save(
            Aktør(aktørId = aktørIdStr).also {
                it.personidenter.add(
                    Personident(fødselsnummer = fødselsnummer, aktør = it)
                )
            }
        )

    private fun opprettPersonIdent(aktør: Aktør, fødselsnummer: String): Aktør {
        aktør.personidenter.filter { it.aktiv }.map {
            it.aktiv = false
            it.gjelderTil = LocalDateTime.now()
        }
        aktør.personidenter.add(
            Personident(fødselsnummer = fødselsnummer, aktør = aktør)
        )
        return aktørIdRepository.save(aktør)
    }

    private fun filtrerAktivtFødselsnummer(identerFraPdl: List<IdentInformasjon>) =
        (
            identerFraPdl.singleOrNull { it.gruppe == "FOLKEREGISTERIDENT" }?.ident
                ?: throw Error("Finner ikke aktiv ident i Pdl")
            )

    private fun filtrerAktørId(identerFraPdl: List<IdentInformasjon>) =
        (
            identerFraPdl.singleOrNull { it.gruppe == "AKTORID" }?.ident
                ?: throw Error("Finner ikke aktørId i Pdl")
            )

    companion object {
        val logger = LoggerFactory.getLogger(PersonidentService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
