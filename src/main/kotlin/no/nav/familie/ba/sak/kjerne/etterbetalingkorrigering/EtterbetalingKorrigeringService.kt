package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class EtterbetalingKorrigeringService(
    private val etterbetalingKorrigeringRepository: EtterbetalingKorrigeringRepository,
    private val loggService: LoggService
) {

    fun finnAktivtKorrigeringPåBehandling(behandlingId: Long): EtterbetalingKorrigering? =
        etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandlingId)

    fun finnAlleKorrigeringerPåBehandling(behandlingId: Long): List<EtterbetalingKorrigering> =
        etterbetalingKorrigeringRepository.finnAlleKorrigeringerPåBehandling(behandlingId)

    @Transactional
    fun lagreEtterbetalingKorrigering(etterbetalingKorrigering: EtterbetalingKorrigering): EtterbetalingKorrigering {
        val behandling = etterbetalingKorrigering.behandling

        finnAktivtKorrigeringPåBehandling(behandling.id)?.let {
            it.aktiv = false
            etterbetalingKorrigeringRepository.save(it)
        }

        loggService.opprettEtterbetalingKorrigeringLogg(behandling, "Etterbetaling i brev er korrigert.")
        return etterbetalingKorrigeringRepository.save(etterbetalingKorrigering)
    }

    @Transactional
    fun settKorrigeringPåBehandlingTilInaktiv(behandling: Behandling): EtterbetalingKorrigering? =
        finnAktivtKorrigeringPåBehandling(behandling.id)?.let {
            loggService.opprettEtterbetalingKorrigeringLogg(behandling, "Etterbetaling korrigering i brev er angret.")
            it.aktiv = false
            etterbetalingKorrigeringRepository.save(it)
        }
}
