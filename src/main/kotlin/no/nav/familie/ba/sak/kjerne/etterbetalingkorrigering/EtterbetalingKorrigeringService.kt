package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class EtterbetalingKorrigeringService(
    private val etterbetalingKorrigeringRepository: EtterbetalingKorrigeringRepository
) {

    fun finnAktivtKorrigeringPåBehandling(behandlingId: Long) =
        etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandlingId)

    fun hentAlleKorrigeringerPåBehandling(behandlingId: Long) =
        etterbetalingKorrigeringRepository.hentAlleKorrigeringPåBehandling(behandlingId)

    @Transactional
    fun lagreKorrigeringPåBehandling(etterbetalingKorrigering: EtterbetalingKorrigering): EtterbetalingKorrigering {
        settKorrigeringPåBehandlingTilInaktiv(etterbetalingKorrigering.behandling.id)

        return etterbetalingKorrigeringRepository.save(etterbetalingKorrigering)
    }

    @Transactional
    fun settKorrigeringPåBehandlingTilInaktiv(behandlingId: Long) =
        finnAktivtKorrigeringPåBehandling(behandlingId)?.let {
            it.aktiv = false
            etterbetalingKorrigeringRepository.save(it)
        }
}