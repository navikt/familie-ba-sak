package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.kontrakter.felles.PersonIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val pdlIdentRestClient: PdlIdentRestClient,
    private val taskRepository: TaskRepositoryWrapper,
) {

    fun hentIdenter(personIdent: String, historikk: Boolean): List<IdentInformasjon> {
        return pdlIdentRestClient.hentIdenter(personIdent, historikk)
    }

    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = hentIdenter(nyIdent.ident, false)
        val aktørId = filtrerAktørId(identerFraPdl)

        val aktør = aktørIdRepository.findByAktørIdOrNull(aktørId)

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
        taskRepository.save(IdentHendelseTask.opprettTask(nyIdent))
    }

    fun hentAlleFødselsnummerForEnAktør(aktør: Aktør) =
        hentIdenter(aktør.aktivFødselsnummer(), true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }

    fun hentOgLagreAktør(ident: String, lagre: Boolean = true): Aktør {
        // Noter at ident kan være både av typen aktørid eller fødselsnummer (d- og f nummer)
        val personident = personidentRepository.findByFødselsnummerOrNull(ident)
        if (personident != null) {
            return personident.aktør
        }

        val aktørIdent = aktørIdRepository.findByAktørIdOrNull(ident)
        if (aktørIdent != null) {
            return aktørIdent
        }

        val identerFraPdl = hentIdenter(ident, false)
        val fødselsnummerAktiv = filtrerAktivtFødselsnummer(identerFraPdl)
        val aktørIdStr = filtrerAktørId(identerFraPdl)

        val personidentPersistert = personidentRepository.findByFødselsnummerOrNull(fødselsnummerAktiv)
        if (personidentPersistert != null) {
            return personidentPersistert.aktør
        }

        val aktørPersistert = aktørIdRepository.findByAktørIdOrNull(aktørIdStr)
        if (aktørPersistert != null) {
            return opprettPersonIdent(aktørPersistert, fødselsnummerAktiv, lagre)
        }

        return opprettAktørIdOgPersonident(aktørIdStr, fødselsnummerAktiv, lagre)
    }

    fun hentOgLagreAktørIder(barnasFødselsnummer: List<String>): List<Aktør> {
        return barnasFødselsnummer.map { hentOgLagreAktør(it) }
    }

    // TODO: Skriv test for denne metoden når den tas i bruk.
    fun hentGjeldendeFødselsnummerForTidspunkt(aktør: Aktør, tidspunkt: LocalDateTime): String {
        val alleIdenter = personidentRepository.hentAlleIdenterForAktørid(aktør.aktørId)
        if (alleIdenter.size == 1) return alleIdenter.first().fødselsnummer
        return alleIdenter.filter { it.gjelderTil?.isAfter(tidspunkt) ?: false }
            .minByOrNull { it.gjelderTil!! }?.fødselsnummer
            ?: alleIdenter.first { it.aktiv }.fødselsnummer
    }

    private fun opprettAktørIdOgPersonident(aktørIdStr: String, fødselsnummer: String, lagre: Boolean): Aktør {
        val aktør = Aktør(aktørId = aktørIdStr).also {
            it.personidenter.add(
                Personident(fødselsnummer = fødselsnummer, aktør = it)
            )
        }

        return if (lagre) {
            aktørIdRepository.saveAndFlush(aktør)
        } else {
            aktør
        }
    }

    private fun opprettPersonIdent(aktør: Aktør, fødselsnummer: String, lagre: Boolean = true): Aktør {
        aktør.personidenter.filter { it.aktiv }.map {
            it.aktiv = false
            it.gjelderTil = LocalDateTime.now()
        }
        // Ekstra persistering eller kommer unique constraint feile.
        if (lagre) aktørIdRepository.saveAndFlush(aktør)
        aktør.personidenter.add(
            Personident(fødselsnummer = fødselsnummer, aktør = aktør)
        )
        return if (lagre) {
            aktørIdRepository.saveAndFlush(aktør)
        } else {
            aktør
        }
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
