package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class KorrigertEtterbetalingService(
    private val korrigertEtterbetalingRepository: KorrigertEtterbetalingRepository,
    private val loggService: LoggService
) {

    fun finnAktivtKorrigeringPåBehandling(behandlingId: BehandlingId): KorrigertEtterbetaling? =
        korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandlingId.id)

    fun finnAlleKorrigeringerPåBehandling(behandlingId: BehandlingId): List<KorrigertEtterbetaling> =
        korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandlingId.id)

    @Transactional
    fun lagreKorrigertEtterbetaling(korrigertEtterbetaling: KorrigertEtterbetaling): KorrigertEtterbetaling {
        val behandling = korrigertEtterbetaling.behandling

        finnAktivtKorrigeringPåBehandling(behandling.behandlingId)?.let {
            it.aktiv = false
            korrigertEtterbetalingRepository.saveAndFlush(it)
        }

        loggService.opprettKorrigertEtterbetalingLogg(behandling, korrigertEtterbetaling)
        return korrigertEtterbetalingRepository.save(korrigertEtterbetaling)
    }

    @Transactional
    fun settKorrigeringPåBehandlingTilInaktiv(behandling: Behandling): KorrigertEtterbetaling? =
        finnAktivtKorrigeringPåBehandling(behandling.behandlingId)?.apply {
            aktiv = false
            loggService.opprettKorrigertEtterbetalingLogg(behandling, this)
        }
}
