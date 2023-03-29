package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KorrigertVedtakService(
    private val korrigertVedtakRepository: KorrigertVedtakRepository,
    private val loggService: LoggService
) {

    fun finnAktivtKorrigertVedtakPåBehandling(behandlingId: BehandlingId): KorrigertVedtak? =
        korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandlingId.id)

    @Transactional
    fun lagreKorrigertVedtak(korrigertVedtak: KorrigertVedtak): KorrigertVedtak {
        val behandling = korrigertVedtak.behandling

        finnAktivtKorrigertVedtakPåBehandling(behandling.behandlingId)?.let {
            it.aktiv = false
            korrigertVedtakRepository.saveAndFlush(it)
        }

        loggService.opprettKorrigertVedtakLogg(behandling, korrigertVedtak)
        return korrigertVedtakRepository.save(korrigertVedtak)
    }

    @Transactional
    fun settKorrigertVedtakPåBehandlingTilInaktiv(behandling: Behandling): KorrigertVedtak? =
        finnAktivtKorrigertVedtakPåBehandling(behandling.behandlingId)?.apply {
            aktiv = false
            loggService.opprettKorrigertVedtakLogg(behandling, this)
        }
}
