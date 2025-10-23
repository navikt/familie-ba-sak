package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository.FagsakIdBehandlingIdOgKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Visningsbehandling
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BehandlingHentOgPersisterService(
    private val behandlingRepository: BehandlingRepository,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
    private val vedtakRepository: VedtakRepository,
) {
    fun lagreEllerOppdater(
        behandling: Behandling,
        sendTilDvh: Boolean = true,
    ): Behandling =
        behandlingRepository.save(behandling).also {
            if (sendTilDvh) {
                saksstatistikkEventPublisher.publiserBehandlingsstatistikk(it.id)
            }
        }

    fun lagreOgFlush(behandling: Behandling): Behandling = behandlingRepository.saveAndFlush(behandling)

    fun finnAktivForFagsak(fagsakId: Long): Behandling? = behandlingRepository.findByFagsakAndAktiv(fagsakId)

    fun finnAktivOgÅpenForFagsak(fagsakId: Long): Behandling? = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId)

    fun erÅpenBehandlingPåFagsak(fagsakId: Long): Boolean = finnAktivOgÅpenForFagsak(fagsakId) != null

    fun hent(behandlingId: Long): Behandling =
        behandlingRepository.finnBehandlingNullable(behandlingId)
            ?: throw Feil("Finner ikke behandling med id $behandlingId")

    fun hentStatus(behandlingId: Long): BehandlingStatus = behandlingRepository.finnStatus(behandlingId)

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? = behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)

    fun hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder: Collection<Long>): Map<Long, Behandling> = behandlingRepository.finnSisteIverksatteBehandlingForFagsaker(fagsakIder = fagsakIder).associate { it.fagsak.id to it }

    /**
     * Henter siste iverksatte behandling FØR en gitt behandling.
     * Bør kun brukes i forbindelse med oppdrag mot økonomisystemet
     * eller ved behandlingsresultat.
     */
    fun hentForrigeBehandlingSomErIverksatt(behandling: Behandling): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeIverksatteBehandling(iverksatteBehandlinger, behandling)
    }

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? {
        val behandlingerPåFagsak = behandlingRepository.finnBehandlinger(fagsakId)
        return behandlingerPåFagsak.hentSisteSomErVedtatt()
    }

    fun hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(fagsakIder: Set<Long>): List<Behandling> {
        val behandlingerPåFagsakene =
            behandlingRepository.finnBehandlinger(fagsakIder)

        return behandlingerPåFagsakene
            .groupBy { it.fagsak.id }
            .mapNotNull { (_, behandling) -> behandling.hentSisteSomErSentTilØkonomi() }
    }

    private fun List<Behandling>.hentSisteSomErVedtatt() =
        filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }

    private fun List<Behandling>.hentSisteSomErSentTilØkonomi() =
        filter { !it.erHenlagt() && (it.status == BehandlingStatus.AVSLUTTET || it.status == BehandlingStatus.IVERKSETTER_VEDTAK) }
            .maxByOrNull { it.aktivertTidspunkt }

    /**
     * Henter siste behandling som er vedtatt FØR en gitt behandling
     */
    fun hentForrigeBehandlingSomErVedtatt(behandling: Behandling): Behandling? {
        val behandlinger = behandlingRepository.finnBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeBehandlingSomErVedtatt(behandlinger, behandling)
    }

    fun <T> partitionByIverksatteBehandlinger(funksjon: (iverksatteBehandlinger: List<Long>) -> List<T>): List<T> =
        behandlingRepository
            .finnSisteIverksatteBehandlingFraLøpendeFagsaker()
            .chunked(10000)
            .flatMap { funksjon(it) }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(): List<Long> = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()

    fun hentSisteIverksatteBehandlingerFraLøpendeEøsFagsaker(page: Pageable): Page<FagsakIdBehandlingIdOgKategori> = behandlingRepository.finnSisteIverksatteBehandlingForLøpendeEøsFagsaker(page)

    fun hentAlleFagsakerMedLøpendeValutakursIMåned(måned: YearMonth): List<Long> = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned.førsteDagIInneværendeMåned())

    fun hentBehandlinger(fagsakId: Long): List<Behandling> = behandlingRepository.finnBehandlinger(fagsakId)

    fun hentVisningsbehandlinger(fagsakId: Long): List<Visningsbehandling> =
        behandlingRepository
            .finnBehandlinger(fagsakId)
            // Fjerner behandlinger med opprettetÅrsak = OPPDATER_UTVIDET_KLASSEKODE. Dette er kun en teknisk greie og ikke noe saksbehandler trenger å forholde seg til.
            .filter { !it.erOppdaterUtvidetKlassekode() }
            .map { Visningsbehandling.opprettFraBehandling(it, vedtakRepository.finnVedtaksdatoForBehandling(it.id)) }

    fun hentBehandlinger(
        fagsakId: Long,
        status: BehandlingStatus,
    ): List<Behandling> = behandlingRepository.finnBehandlinger(fagsakId, status)

    fun hentFerdigstilteBehandlinger(fagsakId: Long): List<Behandling> = hentBehandlinger(fagsakId).filter { it.erVedtatt() }

    fun hentAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): Map<Long, String> = behandlingRepository.finnAktivtFødselsnummerForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentTssEksternIdForBehandlinger(behandlingIder: List<Long>): Map<Long, String> = behandlingRepository.finnTssEksternIdForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> = behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsakId)
}
