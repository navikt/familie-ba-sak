package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService.Companion.logger
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.person.pdl.aktor.v2.Type
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class HåndterNyIdentService(
    private val aktørIdRepository: AktørIdRepository,
    private val fagsakService: FagsakService,
    private val opprettTaskService: OpprettTaskService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personopplysningerService: PersonopplysningerService,
    private val behandlingRepository: BehandlingRepository,
    private val personIdentService: PersonidentService,
) {
    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = personIdentService.hentIdenter(nyIdent.ident, true)

        val aktørId = identerFraPdl.hentAktivAktørId()

        mergeIdentOgRekjørSenere(identerFraPdl)

        val aktør = aktørIdRepository.findByAktørIdOrNull(aktørId)

        return if (aktør?.harIdent(fødselsnummer = nyIdent.ident) == false) {
            logger.info("Legger til ny ident")
            secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
            personIdentService.opprettPersonIdent(aktør, nyIdent.ident)
        } else {
            aktør
        }
    }

    private fun mergeIdentOgRekjørSenere(alleIdenterFraPdl: List<IdentInformasjon>) {
        val alleHistoriskeAktørIder = alleIdenterFraPdl.filter { it.gruppe == Type.AKTORID.name && it.historisk }.map { it.ident }

        val aktiveAktørerForHistoriskAktørIder =
            alleHistoriskeAktørIder
                .mapNotNull { aktørId -> aktørIdRepository.findByAktørIdOrNull(aktørId) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        if (aktiveAktørerForHistoriskAktørIder.isEmpty()) {
            return
        }

        val alleAktørerMedAktivPersonIdent =
            alleIdenterFraPdl
                .filter { it.gruppe == Type.AKTORID.name }
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it.ident) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        val fagsakId = validerKunÉnFagsakId(alleAktørerMedAktivPersonIdent)

        validerUendretFødselsdatoFraForrigeBehandling(alleIdenterFraPdl, fagsakId)

        val task =
            opprettTaskService.opprettTaskForÅPatcheMergetIdent(
                PatchMergetIdentDto(
                    fagsakId = fagsakId,
                    nyIdent = PersonIdent(alleIdenterFraPdl.first { it.gruppe == "FOLKEREGISTERIDENT" && !it.historisk }.ident),
                    gammelIdent = PersonIdent(alleIdenterFraPdl.first { it.gruppe == "FOLKEREGISTERIDENT" && it.historisk }.ident),
                ),
            )

        secureLogger.info("Potensielt merget ident for $alleIdenterFraPdl")
        throw RekjørSenereException(
            årsak = "Mottok identhendelse som blir forsøkt patchet automatisk: ${task.id}. Prøver å rekjøre etter patching av merget ident. Se secure logger for mer info.",
            triggerTid = LocalDateTime.now().plusHours(1),
        )
    }

    private fun validerKunÉnFagsakId(aktiveAktørerForHistoriskAktørIder: List<Aktør>): Long {
        val fagsakDeltagere =
            aktiveAktørerForHistoriskAktørIder
                .flatMap { aktør ->
                    aktør.personidenter.flatMap { ident ->
                        fagsakService.hentFagsakDeltager(ident.fødselsnummer)
                    }
                }

        when {
            fagsakDeltagere.isEmpty() -> throw Feil("Fant ingen fagsaker på identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
            fagsakDeltagere.map { it.fagsakId }.distinct().size > 1 -> throw Feil("Det eksisterer flere fagsaker på identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
            fagsakDeltagere.first().fagsakId == null ->
                throw Feil("Fant ingen fagsakId på fagsak for identer som skal merges: ${aktiveAktørerForHistoriskAktørIder.first()}. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
        }

        return fagsakDeltagere.first().fagsakId!!
    }

    private fun validerUendretFødselsdatoFraForrigeBehandling(
        alleIdenterFraPdl: List<IdentInformasjon>,
        fagsakId: Long,
    ) {
        val aktivAktørIdent = alleIdenterFraPdl.hentAktivAktørId()
        val aktivAktør =
            aktørIdRepository.findByAktørIdOrNull(aktivAktørIdent)
                ?: throw Feil("Finnes ingen aktiv aktør")

        val fødselsDatoFraPdl = personopplysningerService.hentPersoninfoEnkel(aktivAktør).fødselsdato

        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId) ?: return

        val personGrunnlag = persongrunnlagService.hentAktiv(forrigeBehandling.id)!!
        val fødselsdatoForrigeBehandling =
            personGrunnlag.personer.singleOrNull { it.aktør.aktørId == aktivAktørIdent }?.fødselsdato
                ?: throw Feil("Aktør $aktivAktørIdent fantes ikke i behandling $forrigeBehandling")

        if (fødselsDatoFraPdl != fødselsdatoForrigeBehandling) {
            throw Feil("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt. Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info.")
        }
    }
}
