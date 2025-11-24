package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val stegService: StegService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val klagebehandlingHenter: KlagebehandlingHenter,
    private val klagebehandlingOppretter: KlagebehandlingOppretter,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettKlage(
        fagsakId: Long,
        opprettKlageDto: OpprettKlageDto,
    ): UUID = klagebehandlingOppretter.opprettKlage(fagsakId, opprettKlageDto)

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottattDato: LocalDate,
    ): UUID = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> = klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId)

    fun hentForrigeVedtatteKlagebehandling(behandling: Behandling): KlagebehandlingDto? = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

    @Transactional(readOnly = true)
    fun kanOppretteRevurdering(fagsakId: Long): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> {
                KanOppretteRevurderingResponse(true, null)
            }

            is KanIkkeOppretteRevurdering -> {
                KanOppretteRevurderingResponse(
                    false,
                    resultat.årsak.kanIkkeOppretteRevurderingÅrsak,
                )
            }
        }
    }

    @Transactional
    fun validerOgOpprettRevurderingKlage(
        fagsakId: Long,
        klagebehandlingId: UUID,
    ): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> opprettRevurderingKlage(fagsak, klagebehandlingId)
            is KanIkkeOppretteRevurdering -> OpprettRevurderingResponse(IkkeOpprettet(resultat.årsak.ikkeOpprettetÅrsak))
        }
    }

    private fun opprettRevurderingKlage(
        fagsak: Fagsak,
        klagebehandlingId: UUID,
    ) = try {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)
                ?: throw Feil("Finner ikke tidligere behandling")

        val nyBehandling =
            NyBehandling(
                kategori = forrigeBehandling.kategori,
                underkategori = forrigeBehandling.underkategori,
                søkersIdent = forrigeBehandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.KLAGE,
                navIdent = SikkerhetContext.hentSaksbehandler(),
                // barnasIdenter hentes fra forrige behandling i håndterNyBehandling() ved revurdering
                barnasIdenter = emptyList(),
                fagsakId = forrigeBehandling.fagsak.id,
                nyEksternBehandlingRelasjon = NyEksternBehandlingRelasjon.opprettForKlagebehandling(klagebehandlingId),
            )

        val revurdering = stegService.håndterNyBehandling(nyBehandling)
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

        val finnesVedtattBehandlingPåFagsak =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id) != null
        if (!finnesVedtattBehandlingPåFagsak) {
            return KanIkkeOppretteRevurdering(Årsak.INGEN_BEHANDLING)
        }
        return KanOppretteRevurdering
    }

    fun hentFagsystemVedtak(fagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        val behandlinger = behandlingHentOgPersisterService.hentFerdigstilteBehandlinger(fagsak.id)
        val ferdigstilteBaBehandlinger = behandlinger.map { it.tilFagsystemVedtak() }

        val vedtakTilbakekreving = tilbakekrevingKlient.hentTilbakekrevingsvedtak(fagsakId)

        return ferdigstilteBaBehandlinger + vedtakTilbakekreving
    }

    private fun Behandling.tilFagsystemVedtak(): FagsystemVedtak {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(id)

        return FagsystemVedtak(
            eksternBehandlingId = this.id.toString(),
            behandlingstype = this.type.visningsnavn,
            resultat = this.resultat.displayName,
            vedtakstidspunkt = vedtak.vedtaksdato ?: throw Feil("Mangler vedtakstidspunkt for behandling=$id"),
            fagsystemType = FagsystemType.ORDNIÆR,
            regelverk = this.kategori.tilRegelverk(),
        )
    }
}

private sealed interface KanOppretteRevurderingResultat

private object KanOppretteRevurdering : KanOppretteRevurderingResultat

private data class KanIkkeOppretteRevurdering(
    val årsak: Årsak,
) : KanOppretteRevurderingResultat

private enum class Årsak(
    val ikkeOpprettetÅrsak: IkkeOpprettetÅrsak,
    val kanIkkeOppretteRevurderingÅrsak: KanIkkeOppretteRevurderingÅrsak,
) {
    ÅPEN_BEHANDLING(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING),
    INGEN_BEHANDLING(IkkeOpprettetÅrsak.INGEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING),
}
