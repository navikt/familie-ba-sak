package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class BehandlingHentOgPersisterService(
    private val behandlingRepository: BehandlingRepository,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher
) {
    fun lagreEllerOppdater(behandling: Behandling, sendTilDvh: Boolean = true): Behandling {
        return behandlingRepository.save(behandling).also {
            if (sendTilDvh) {
                saksstatistikkEventPublisher.publiserBehandlingsstatistikk(it.id)
            }
        }
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hentAktivOgÅpenForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId)
    }

    fun erÅpenBehandlingPåFagsak(fagsakId: Long): Boolean {
        return hentAktivOgÅpenForFagsak(fagsakId) != null
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandlingNullable(behandlingId)
            ?: throw Feil("Finner ikke behandling med id $behandlingId")
    }

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    /**
     * Henter siste iverksatte behandling FØR en gitt behandling.
     * Bør kun brukes i forbindelse med oppdrag mot økonomisystemet
     * eller ved behandlingsresultat.
     */
    fun hentForrigeBehandlingSomErIverksatt(behandling: Behandling): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeIverksatteBehandling(iverksatteBehandlinger, behandling)
    }

    fun hentForrigeBehandlingSomErIverksattFraBehandlingsId(behandlingId: Long): Behandling? {
        val behandling = hent(behandlingId)
        return hentForrigeBehandlingSomErIverksatt(behandling)
    }

    /**
     * Henter iverksatte behandlinger FØR en gitt behandling.
     * Bør kun brukes i forbindelse med oppdrag mot økonomisystemet
     * eller ved behandlingsresultat.
     */
    fun hentBehandlingerSomErIverksatt(behandling: Behandling): List<Behandling> {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentIverksatteBehandlinger(iverksatteBehandlinger, behandling)
    }

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? {
        return behandlingRepository.finnBehandlinger(fagsakId)
            .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
            .maxByOrNull { it.opprettetTidspunkt }
    }

    /**
     * Henter siste behandling som er vedtatt FØR en gitt behandling
     */
    fun hentForrigeBehandlingSomErVedtatt(behandling: Behandling): Behandling? {
        val behandlinger = behandlingRepository.finnBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeBehandlingSomErVedtatt(behandlinger, behandling)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(page: Pageable): Page<BigInteger> =
        behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(page)

    fun <T> partitionByIverksatteBehandlinger(funksjon: (iverksatteBehandlinger: List<Long>) -> List<T>): List<T> {
        return behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker().chunked(10000)
            .flatMap { funksjon(it) }
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(): List<Long> =
        behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun hentFerdigstilteBehandlinger(fagsakId: Long): List<Behandling> =
        hentBehandlinger(fagsakId).filter { it.erVedtatt() }

    fun hentAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): Map<Long, String> =
        behandlingRepository.finnAktivtFødselsnummerForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentTssEksternIdForBehandlinger(behandlingIder: List<Long>): Map<Long, String> =
        behandlingRepository.finnTssEksternIdForBehandlinger(behandlingIder).associate { it.first to it.second }

    fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnIverksatteBehandlinger(fagsakId = fagsakId)
    }
}
