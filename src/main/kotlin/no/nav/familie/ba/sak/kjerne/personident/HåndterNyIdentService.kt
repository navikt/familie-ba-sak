package no.nav.familie.ba.sak.kjerne.personident

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
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
        val aktuellFagsakVedMerging = hentAktuellFagsakForIdenthendelse(identerFraPdl)

        return when {
            // Personen er ikke i noen fagsaker
            aktuellFagsakVedMerging == null -> aktør

            // Ny aktørId, nytt fødselsnummer -> begge håndteres i PatchMergetIdentTask
            aktør == null -> {
                validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, aktuellFagsakVedMerging)
                opprettMergeIdentTask(aktuellFagsakVedMerging.id, identerFraPdl)
                null
            }

            // Samme aktørId, nytt fødselsnummer -> legg til fødselsnummer på aktør
            !aktør.harIdent(fødselsnummer = nyIdent.ident) -> {
                validerUendretFødselsdatoFraForrigeBehandling(identerFraPdl, aktuellFagsakVedMerging)
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

    private fun hentAktuellFagsakForIdenthendelse(alleIdenterFraPdl: List<IdentInformasjon>): Fagsak? {
        val aktørerMedAktivPersonident =
            alleIdenterFraPdl
                .hentAktørIder()
                .mapNotNull { aktørIdRepository.findByAktørIdOrNull(it) }
                .filter { aktør -> aktør.personidenter.any { personident -> personident.aktiv } }

        val fagsaker =
            aktørerMedAktivPersonident
                .flatMap { aktør -> fagsakService.hentFagsakerPåPerson(aktør) + fagsakService.hentAlleFagsakerForAktør(aktør) }

        if (fagsaker.toSet().size > 1) {
            secureLogger.warn(
                "Det eksisterer flere fagsaker på identer som skal merges $fagsaker.\n" +
                    "Identer: ${alleIdenterFraPdl.filter { it.gruppe == Type.FOLKEREGISTERIDENT.name }}",
            )
            throw Feil("Det eksisterer flere fagsaker på identer som skal merges. Patch manuelt. Info om fagsaker og identer ligger i securelog. $LENKE_INFO_OM_MERGING")
        }

        return fagsaker.firstOrNull()
    }

    private fun validerUendretFødselsdatoFraForrigeBehandling(
        alleIdenterFraPdl: List<IdentInformasjon>,
        fagsak: Fagsak,
    ) {
        // Hvis søkers fødselsdato endrer seg kan vi alltid patche siden det ikke påvirker andeler. Med mindre søker er enslig mindreårig.
        val søkersAktørId = fagsak.aktør.aktørId
        if (fagsak.type != FagsakType.BARN_ENSLIG_MINDREÅRIG && søkersAktørId in alleIdenterFraPdl.hentAktørIder()) return

        val aktivFødselsnummer = alleIdenterFraPdl.hentAktivFødselsnummer()
        val fødselsdatoFraPdl = pdlRestClient.hentPerson(aktivFødselsnummer, PersonInfoQuery.ENKEL).fødselsdato

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
            secureLogger.warn(
                """Fødselsdato er forskjellig fra forrige behandling.
                    Ny fødselsdato $fødselsdatoFraPdl, forrige fødselsdato $fødselsdatoForrigeBehandling
                    Fagsak: ${forrigeBehandling.fagsak.id}
                    Ny ident: ${alleIdenterFraPdl.filter { !it.historisk && it.gruppe == Type.FOLKEREGISTERIDENT.name }.map { it.ident }}
                    Gamle identer: ${alleIdenterFraPdl.filter { it.historisk && it.gruppe == Type.FOLKEREGISTERIDENT.name }.map { it.ident }}
                    Send informasjonen beskrevet over til en fagressurs og patch identen manuelt. Se lenke for mer info:
                    ${LENKE_INFO_OM_MERGING}
                """.trimMargin(),
            )
            throw Feil("Fødselsdato er forskjellig fra forrige behandling. Kopier tekst fra securelog og send til en fagressurs")
        }
    }

    companion object {
        const val LENKE_INFO_OM_MERGING: String =
            "Se https://github.com/navikt/familie/blob/main/doc/ba-sak/manuellt-patche-akt%C3%B8r-sak.md#manuell-patching-av-akt%C3%B8r-for-en-behandling for mer info."
    }
}
