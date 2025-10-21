package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivAktørId
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktivFødselsnummer
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.hentAktørIder
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService.Companion.logger
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.Task
import no.nav.person.pdl.aktor.v2.Type
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class HåndterNyIdentService(
    private val aktørIdRepository: AktørIdRepository,
    private val fagsakService: FagsakService,
    private val opprettTaskService: OpprettTaskService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlinghentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestKlient: PdlRestKlient,
    private val personIdentService: PersonidentService,
) {
    @Transactional
    fun håndterNyIdent(nyIdent: PersonIdent): Aktør? {
        logger.info("Håndterer ny ident")
        secureLogger.info("Håndterer ny ident ${nyIdent.ident}")
        val identerFraPdl = personIdentService.hentIdenter(nyIdent.ident, true)

        val aktørId = identerFraPdl.hentAktivAktørId()
        val aktør = aktørIdRepository.findByAktørIdOrNull(aktørId)
        val aktuelleFagsakerVedMerging = hentAktuelleFagsakerForIdenthendelse(identerFraPdl)
        if (aktuelleFagsakerVedMerging.size > 1) {
            logger.info("Fant mer enn 1 fagsak ved patching av ident")
        }

        return when {
            // Personen er ikke i noen fagsaker
            aktuelleFagsakerVedMerging.isNullOrEmpty() -> aktør

            // Ny aktørId, nytt fødselsnummer -> begge håndteres i PatchMergetIdentTask
            aktør == null -> {
                aktuelleFagsakerVedMerging.forEach { fagsak ->
                    validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, fagsak)
                }

                // patcheendepunktet trenger en fagsak, men samme hvilken fagsak
                opprettMergeIdentTask(aktuelleFagsakerVedMerging.first().id, identerFraPdl)
                null
            }

            // Samme aktørId, nytt fødselsnummer -> legg til fødselsnummer på aktør
            !aktør.harIdent(fødselsnummer = nyIdent.ident) -> {
                aktuelleFagsakerVedMerging.forEach { fagsak ->
                    validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, fagsak)
                }
                logger.info("Legger til ny ident")
                secureLogger.info("Legger til ny ident ${nyIdent.ident} på aktør ${aktør.aktørId}")
                personIdentService.opprettPersonIdent(aktør, nyIdent.ident, true)
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

    private fun hentAktuelleFagsakerForIdenthendelse(alleIdenterFraPdl: List<IdentInformasjon>): List<Fagsak> {
        val aktørerMedAktivPersonident =
            alleIdenterFraPdl
                .hentAktørIder()
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        return aktørerMedAktivPersonident
            .flatMap { aktør -> fagsakService.hentFagsakerPåPerson(aktør) + fagsakService.hentAlleFagsakerForAktør(aktør) }
    }

    private fun validerUendretFødselsdatoFraForrigeBehandling(
        alleIdenterFraPdl: List<IdentInformasjon>,
        fagsak: Fagsak,
    ) {
        // Hvis søkers fødselsdato endrer seg kan vi alltid patche siden det ikke påvirker andeler. Med mindre søker er enslig mindreårig.
        val søkersAktørId = fagsak.aktør.aktørId
        if (fagsak.type != FagsakType.BARN_ENSLIG_MINDREÅRIG && søkersAktørId in alleIdenterFraPdl.hentAktørIder()) return

        val aktivFødselsnummer = alleIdenterFraPdl.hentAktivFødselsnummer()
        val fødselsdatoFraPdl = pdlRestKlient.hentPerson(aktivFødselsnummer, PersonInfoQuery.ENKEL).fødselsdato

        val forrigeBehandling =
            behandlinghentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id)
                ?: return // Hvis det ikke er noen tidligere behandling kan vi patche uansett

        val aktørIder = alleIdenterFraPdl.hentAktørIder()
        val personGrunnlag = persongrunnlagService.hentAktiv(forrigeBehandling.id) ?: throw Feil("Fant ikke persongrunnlag for behandling med id ${forrigeBehandling.id}")
        val fødselsdatoForrigeBehandling =
            personGrunnlag.personer.singleOrNull { it.aktør.aktørId in aktørIder }?.fødselsdato
                ?: return // Hvis aktør ikke er med i forrige behandling kan vi patche selv om fødselsdato er ulik

        // Hvis begge fødseldatoene er eldre enn 18 år kan vi patche uansett
        if (fødselsdatoFraPdl.isBefore(LocalDate.now().minusYears(18)) && fødselsdatoForrigeBehandling.isBefore(LocalDate.now().minusYears(18))) {
            secureLogger.info("$fødselsdatoFraPdl og $fødselsdatoForrigeBehandling er eldre enn 18 år. Kan patche uansett")
            return
        }

        if (fødselsdatoFraPdl.toYearMonth() != fødselsdatoForrigeBehandling.toYearMonth()) {
            val introTekstOmFeil =
                "Fødselsdato er forskjellig fra forrige behandling. \n" +
                    "   Ny fødselsdato $fødselsdatoFraPdl, forrige fødselsdato $fødselsdatoForrigeBehandling\n" +
                    "   Fagsak: ${forrigeBehandling.fagsak.id} \n"
            secureLogger.warn(
                "$introTekstOmFeil" +
                    "   Ny ident: ${alleIdenterFraPdl.filter { !it.historisk && it.gruppe == Type.FOLKEREGISTERIDENT.name }.map { it.ident }}\n" +
                    "   Gamle identer: ${alleIdenterFraPdl.filter { it.historisk && it.gruppe == Type.FOLKEREGISTERIDENT.name }.map { it.ident }}\n",
            )
            throw Feil(
                "$introTekstOmFeil\n" +
                    "Du MÅ først patche fnr med PatchMergetIdentTask og etterpå sende saken til en fagressurs.\n" +
                    "Info om gammel og nytt fnr finner man i loggmelding med level WARN i securelogs.\n" +
                    "${LENKE_INFO_OM_MERGING}",
            )
        }
    }

    companion object {
        const val LENKE_INFO_OM_MERGING: String =
            "Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info."
    }
}
