package no.nav.familie.ba.sak.ekstern

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternBehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional(readOnly = true)
    fun kanOppretteRevurdering(fagsakId: Long): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> KanOppretteRevurderingResponse(true, null)
            is KanIkkeOppretteRevurdering ->
                KanOppretteRevurderingResponse(false, resultat.årsak.kanIkkeOppretteRevurderingÅrsak)
        }
    }

    @Transactional
    fun opprettRevurderingKlage(fagsakId: Long): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> opprettRevurdering(fagsak, BehandlingÅrsak.KLAGE)
            is KanIkkeOppretteRevurdering ->
                OpprettRevurderingResponse(IkkeOpprettet(resultat.årsak.ikkeOpprettetÅrsak))
        }
    }

    private fun opprettRevurdering(fagsak: Fagsak, behandligÅrsak: BehandlingÅrsak) = try {
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)
            ?: throw Feil("Finner ikke tidligere behandling")

        val nyBehandling = NyBehandling(
            kategori = forrigeBehandling.kategori,
            underkategori = forrigeBehandling.underkategori,
            søkersIdent = forrigeBehandling.fagsak.aktør.aktivFødselsnummer(),
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = behandligÅrsak,
            navIdent = SikkerhetContext.hentSaksbehandler(),

            // barnasIdenter hentes fra forrige behandling i håndterNyBehandling() ved revurdering
            barnasIdenter = emptyList(),

            fagsakId = forrigeBehandling.fagsak.id

        )

        val revurdering = behandlingService.opprettBehandling(nyBehandling)
        OpprettRevurderingResponse(Opprettet(revurdering.id.toString()))
    } catch (e: Exception) {
        logger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}, se secure logg for detaljer")
        secureLogger.error("Feilet opprettelse av revurdering for fagsak=$fagsak", e)
        OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL, e.message))
    }

    private fun utledKanOppretteRevurdering(fagsak: Fagsak): KanOppretteRevurderingResultat {
        val erÅpenBehandlingPåFagsak = behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(fagsak.id)
        if (erÅpenBehandlingPåFagsak) {
            return KanIkkeOppretteRevurdering(Årsak.ÅPEN_BEHANDLING)
        }
        if (!behandlingHentOgPersisterService.erAktivBehandlingPåFagsak(fagsak.id)) {
            return KanIkkeOppretteRevurdering(Årsak.INGEN_BEHANDLING)
        }
        return KanOppretteRevurdering
    }
}

private sealed interface KanOppretteRevurderingResultat
private object KanOppretteRevurdering : KanOppretteRevurderingResultat
private data class KanIkkeOppretteRevurdering(val årsak: Årsak) : KanOppretteRevurderingResultat

private enum class Årsak(
    val ikkeOpprettetÅrsak: IkkeOpprettetÅrsak,
    val kanIkkeOppretteRevurderingÅrsak: KanIkkeOppretteRevurderingÅrsak
) {

    ÅPEN_BEHANDLING(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING),
    INGEN_BEHANDLING(IkkeOpprettetÅrsak.INGEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING),
}
