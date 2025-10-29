package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.saner
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummerOrNull
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.person.pdl.aktor.v2.Type
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PersonidentService(
    private val personidentRepository: PersonidentRepository,
    private val aktørIdRepository: AktørIdRepository,
    private val pdlIdentRestKlient: PdlIdentRestKlient,
    private val taskRepository: TaskRepositoryWrapper,
) {
    fun hentIdenter(
        personIdent: String,
        historikk: Boolean,
    ): List<IdentInformasjon> = pdlIdentRestKlient.hentIdenter(personIdent, historikk)

    fun identSkalLeggesTil(nyIdent: PersonIdent): Boolean {
        val identerFraPdl = hentIdenter(nyIdent.ident, true)
        val aktører =
            identerFraPdl
                .filter { it.gruppe == Type.AKTORID.name }
                .map { it.ident }
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it) }

        if (aktører.isNotEmpty()) {
            return aktører.firstOrNull { it.harIdent(nyIdent.ident) } == null
        }
        return false
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

    fun hentAktørOrNullHvisIkkeAktivFødselsnummer(identEllerAktørId: String): Aktør? =
        hentIdenter(identEllerAktørId, false).hentAktivFødselsnummerOrNull()?.let {
            hentAktør(identEllerAktørId)
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

        secureLogger.info("Henter identer for ident=$ident")
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

    fun opprettPersonIdent(
        aktør: Aktør,
        fødselsnummer: String,
        lagreNyAktør: Boolean = true,
    ): Aktør {
        secureLogger.info("Oppretter personIdent: aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer lagreNyAktør=$lagreNyAktør, personidenter=${aktør.personidenter}")
        val eksisterendePersonIdent = aktør.personidenter.filter { it.fødselsnummer == fødselsnummer && it.aktiv }
        secureLogger.info("Aktøren har fødselsnummer ${aktør.personidenter.map { it.fødselsnummer }}")
        if (eksisterendePersonIdent.isEmpty()) {
            secureLogger.info("Fins ikke eksisterende personIdent for. aktørIdStr=${aktør.aktørId} fødselsnummer=$fødselsnummer lagre=$lagreNyAktør, personidenter=${aktør.personidenter}, så lager ny")
            aktør.personidenter.filter { it.aktiv }.map {
                it.aktiv = false
                it.gjelderTil = LocalDateTime.now()
            }

            if (lagreNyAktør) aktørIdRepository.saveAndFlush(aktør)

            aktør.personidenter.add(
                Personident(fødselsnummer = fødselsnummer, aktør = aktør),
            )
            if (lagreNyAktør) aktørIdRepository.saveAndFlush(aktør)
        }
        return aktør
    }

    companion object {
        val logger = LoggerFactory.getLogger(PersonidentService::class.java)
    }
}
