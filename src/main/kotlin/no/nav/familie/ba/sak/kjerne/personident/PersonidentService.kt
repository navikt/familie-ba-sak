package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.saner
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.zeroSingleOrThrow
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.error.RekjørSenereException
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
    private val fagsakService: FagsakService,
    private val opprettTaskService: OpprettTaskService,
) {
    fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<IdentInformasjon> = pdlIdentRestClient.hentIdenter(personIdent, historikk)

    fun identSkalLeggesTil(nyIdent: PersonIdent): Boolean {
        val identerFraPdl = hentIdenter(nyIdent.ident, true)
        val aktører =
            identerFraPdl
                .filter { it.gruppe == "AKTORID" }
                .map { it.ident }
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it) }

        if (aktører.isNotEmpty()) {
            return aktører.firstOrNull { it.harIdent(nyIdent.ident) } == null
        }
        return false
    }

    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = hentIdenter(nyIdent.ident, true)

        val aktørId = identerFraPdl.hentAktivAktørId()

        validerOmAktørIdErMerget(identerFraPdl)

        val aktør = aktørIdRepository.findByAktørIdOrNull(aktørId)

        return if (aktør?.harIdent(fødselsnummer = nyIdent.ident) == false) {
            logger.info("Legger til ny ident")
            secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
            opprettPersonIdent(aktør, nyIdent.ident)
        } else {
            aktør
        }
    }

    @Transactional
    fun opprettTaskForIdentHendelse(nyIdent: PersonIdent) {
        if (identSkalLeggesTil(nyIdent)) {
            logger.info("Oppretter task for senere håndterering av ny ident")
            secureLogger.info("Oppretter task for senere håndterering av ny ident ${nyIdent.ident}")
            taskRepository.save(IdentHendelseTask.opprettTask(nyIdent))
        } else {
            logger.info("Ident er ikke knyttet til noen av aktørene våre, ignorerer hendelse.")
        }
    }

    fun hentAlleFødselsnummerForEnAktør(aktør: Aktør) =
        hentIdenter(aktør.aktivFødselsnummer(), true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }

    fun hentAktør(identEllerAktørId: String): Aktør {
        val aktør = hentOgLagreAktør(ident = identEllerAktørId, lagre = false)

        if (aktør.personidenter.find { it.aktiv } == null) {
            secureLogger.warn("Fant ikke aktiv ident for aktør med id ${aktør.aktørId} for ident $identEllerAktørId")
            throw Feil("Fant ikke aktiv ident for aktør")
        }

        return aktør
    }

    fun hentOgLagreAktør(
        ident: String,
        lagre: Boolean,
    ): Aktør {
        // Noter at ident kan være både av typen aktørid eller fødselsnummer (d- og f nummer)
        val personident =
            try {
                personidentRepository.findByFødselsnummerOrNull(ident)
            } catch (e: Exception) {
                secureLogger.info("Feil ved henting av ident=${ident.saner()}, lagre=$lagre", e)
                throw e
            }
        if (personident != null) {
            return personident.aktør
        }

        val aktørIdent = aktørIdRepository.findByAktørIdOrNull(ident)
        if (aktørIdent != null) {
            return aktørIdent
        }

        val identerFraPdl = hentIdenter(ident, false)
        val fødselsnummerAktiv = identerFraPdl.hentAktivFødselsnummer()
        val aktørIdStr = identerFraPdl.hentAktivAktørId()

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

    fun hentOgLagreAktørIder(
        barnasFødselsnummer: List<String>,
        lagre: Boolean,
    ): List<Aktør> = barnasFødselsnummer.map { hentOgLagreAktør(it, lagre) }

    fun hentAktørIder(barnasFødselsnummer: List<String>): List<Aktør> = barnasFødselsnummer.map { hentAktør(it) }

    /*
    Ved merge vil èn av de to gjeldende aktør-IDene videreføres som gjeldende. Vi trenger dermed å sjekke
    om det finnes en aktiv personident rad for den gamle aktørId

     */

    private fun validerOmAktørIdErMerget(alleHistoriskeIdenterFraPdl: List<IdentInformasjon>) {
        val alleHistoriskeAktørIder = alleHistoriskeIdenterFraPdl.filter { it.gruppe == "AKTORID" && it.historisk }.map { it.ident }

        val aktiveAktørerForHistoriskAktørIder =
            alleHistoriskeAktørIder
                .mapNotNull { aktørId -> aktørIdRepository.findByAktørIdOrNull(aktørId) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        if (aktiveAktørerForHistoriskAktørIder.isNotEmpty()) {
            val fagsak =
                aktiveAktørerForHistoriskAktørIder
                    .flatMap { aktør ->
                        aktør.personidenter.flatMap { ident ->
                            fagsakService.hentFagsakDeltager(ident.fødselsnummer)
                        }
                    }.zeroSingleOrThrow {
                        throw Feil("Det eksisterer flere fagsaker på identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
                    }

            if (fagsak == null) {
                throw Feil("Fant ingen fagsaker på identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
            }

            if (fagsak.fagsakId == null) {
                throw Feil("Fant ingen fagsakId på fagsak for identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
            }

            validerUendretFødselsdato(alleHistoriskeIdenterFraPdl)

            val task =
                opprettTaskService.opprettTaskForÅPatcheMergetIdent(
                    PatchMergetIdentDto(
                        fagsakId = fagsak.fagsakId,
                        nyIdent = PersonIdent(alleHistoriskeIdenterFraPdl.first { it.gruppe == "FOLKEREGISTERIDENT" && !it.historisk }.ident),
                        gammelIdent = PersonIdent(alleHistoriskeIdenterFraPdl.first { it.gruppe == "FOLKEREGISTERIDENT" && it.historisk }.ident),
                    ),
                )

            secureLogger.info("Potensielt merget ident for $alleHistoriskeIdenterFraPdl")
            throw RekjørSenereException(
                årsak = "Mottok identhendelse som blir forsøkt patchet automatisk: ${task.id}. Prøver å rekjøre etter patching av merget ident. Se secure logger for mer info.",
                triggerTid = LocalDateTime.now().plusHours(1),
            )
        }
    }

    private fun validerUendretFødselsdato(alleHistoriskeIdenterFraPdl: List<IdentInformasjon>) {
        val unikeFødselsnumre = alleHistoriskeIdenterFraPdl.filter { it.gruppe == "FOLKEREGISTERIDENT" }.map { it.ident.substring(0, 6) }.distinct()

        if (unikeFødselsnumre.size > 1) {
            throw Feil("Det er forskjellige fødselsdatoer på identer som skal merges: ${alleHistoriskeIdenterFraPdl.hentAktivAktørId()}")
        }
    }

    private fun opprettAktørIdOgPersonident(
        aktørIdStr: String,
        fødselsnummer: String,
        lagre: Boolean,
    ): Aktør {
        secureLogger.info("Oppretter aktør og personIdent. aktørIdStr=$aktørIdStr fødselsnummer=$fødselsnummer lagre=$lagre")
        val aktør =
            Aktør(aktørId = aktørIdStr).also {
                it.personidenter.add(
                    Personident(fødselsnummer = fødselsnummer, aktør = it),
                )
            }

        return if (lagre) {
            aktørIdRepository.saveAndFlush(aktør)
        } else {
            aktør
        }
    }

    private fun opprettPersonIdent(
        aktør: Aktør,
        fødselsnummer: String,
        lagre: Boolean = true,
    ): Aktør {
        secureLogger.info("Oppretter personIdent. aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer lagre=$lagre, personidenter=${aktør.personidenter}")
        val eksisterendePersonIdent = aktør.personidenter.filter { it.fødselsnummer == fødselsnummer && it.aktiv }
        secureLogger.info("Aktøren har fødselsnummer ${aktør.personidenter.map { it.fødselsnummer } }")
        if (eksisterendePersonIdent.isEmpty()) {
            secureLogger.info("Fins ikke eksisterende personIdent for. aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer lagre=$lagre, personidenter=${aktør.personidenter}, så lager ny")
            aktør.personidenter.filter { it.aktiv }.map {
                it.aktiv = false
                it.gjelderTil = LocalDateTime.now()
            }
            if (lagre) aktørIdRepository.saveAndFlush(aktør) // Må lagre her fordi unik index er en blanding av aktørid og gjelderTil, og hvis man ikke lagerer før man legger til ny, så feiler det pga indexen.

            aktør.personidenter.add(
                Personident(fødselsnummer = fødselsnummer, aktør = aktør),
            )
            if (lagre) aktørIdRepository.saveAndFlush(aktør)
        }
        return aktør
    }

    companion object {
        val logger = LoggerFactory.getLogger(PersonidentService::class.java)
    }
}
