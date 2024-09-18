package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktørIder
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService.Companion.logger
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterNyIdentService(
    private val aktørIdRepository: AktørIdRepository,
    private val fagsakService: FagsakService,
    private val opprettTaskService: OpprettTaskService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlinghentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestClient: PdlRestClient,
    private val personIdentService: PersonidentService,
) {
    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = personIdentService.hentIdenter(nyIdent.ident, true)

        val aktørId = identerFraPdl.hentAktivAktørId()
        val aktør = aktørIdRepository.findByAktørIdOrNull(aktørId)

        return when {
            // Ny aktørId, nytt fødselsnummer -> begge håndteres i PatchMergetIdentTask
            aktør == null -> {
                val aktuellFagsakIdVedMerging = hentAktuellFagsakId(identerFraPdl)
                if (aktuellFagsakIdVedMerging != null) {
                    validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, aktuellFagsakIdVedMerging)
                    opprettMergeIdentTask(aktuellFagsakIdVedMerging, identerFraPdl)
                }
                null
            }

            // Samme aktørId, nytt fødselsnummer -> legg til fødselsnummer på aktør
            !aktør.harIdent(fødselsnummer = nyIdent.ident) -> {
                logger.info("Legger til ny ident")
                secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
                personIdentService.opprettPersonIdent(aktør, nyIdent.ident)
            }

            // Samme aktørId, samme fødselsnummer -> ignorer hendelse
            else -> aktør
        }
    }

    private fun opprettMergeIdentTask(
        fagsakId: Long,
        identerFraPdl: List<IdentInformasjon>,
    ): Task {
        val aktørIder = identerFraPdl.hentAktørIder()
        val gammelIdent =
            persongrunnlagService
                .hentSøkerOgBarnPåFagsak(fagsakId)
                ?.singleOrNull { it.aktør.aktørId in aktørIder }
                ?.aktør
                ?.aktivFødselsnummer()
                ?: throw Feil("Fant ikke gammel ident for aktør ${identerFraPdl.hentAktivAktørId()} på fagsak $fagsakId")

        val task =
            opprettTaskService.opprettTaskForÅPatcheMergetIdent(
                PatchMergetIdentDto(
                    fagsakId = fagsakId,
                    nyIdent = PersonIdent(identerFraPdl.hentAktivFødselsnummer()),
                    gammelIdent = PersonIdent(gammelIdent),
                ),
            )
        secureLogger.info("Potensielt merget ident for $identerFraPdl")
        return task
    }

    private fun hentAktuellFagsakId(alleIdenterFraPdl: List<IdentInformasjon>): Long? {
        val aktørerMedAktivPersonident =
            alleIdenterFraPdl
                .hentAktørIder()
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        val fagsakIder =
            aktørerMedAktivPersonident
                .flatMap { aktør -> aktør.personidenter.flatMap { ident -> fagsakService.hentFagsakDeltager(ident.fødselsnummer) } }
                .mapNotNull { it.fagsakId }

        if (fagsakIder.toSet().size > 1) {
            throw Feil("Det eksisterer flere fagsaker på identer som skal merges: ${aktørerMedAktivPersonident.first()}. $LENKE_INFO_OM_MERGING")
        }

        return fagsakIder.firstOrNull()
    }

    private fun validerUendretFødselsdatoFraForrigeBehandling(
        alleIdenterFraPdl: List<IdentInformasjon>,
        fagsakId: Long,
    ) {
        val aktivFødselsnummer = alleIdenterFraPdl.hentAktivFødselsnummer()
        val fødselsdatoFraPdl = pdlRestClient.hentPerson(aktivFødselsnummer, PersonInfoQuery.ENKEL).fødselsdato

        val forrigeBehandling =
            behandlinghentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)
                ?: return // Hvis det ikke er noen tidligere behandling kan vi patche uansett

        val aktørIder = alleIdenterFraPdl.hentAktørIder()
        val personGrunnlag = persongrunnlagService.hentAktiv(forrigeBehandling.id) ?: throw Feil("Fant ikke persongrunnlag for behandling med id ${forrigeBehandling.id}")
        val fødselsdatoForrigeBehandling =
            personGrunnlag.personer.singleOrNull { it.aktør.aktørId in aktørIder }?.fødselsdato
                ?: return // Hvis aktør ikke er med i forrige behandling kan vi patche selv om fødselsdato er ulik

        if (fødselsdatoFraPdl != fødselsdatoForrigeBehandling) {
            throw Feil("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt. $LENKE_INFO_OM_MERGING")
        }
    }

    companion object {
        const val LENKE_INFO_OM_MERGING: String =
            "Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info."
    }
}
