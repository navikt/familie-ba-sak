package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EtterbetalingKorrigeringRepository : JpaRepository<EtterbetalingKorrigering, Long> {
    @Query("SELECT ek FROM EtterbetalingKorrigering ek JOIN ek.behandling b WHERE b.id = :behandlingId AND ek.aktiv = true")
    fun finnAktivtKorrigeringPåBehandling(behandlingId: Long): EtterbetalingKorrigering?

    @Query("SELECT ek FROM EtterbetalingKorrigering ek JOIN ek.behandling b WHERE b.id = :behandlingId")
    fun finnAlleKorrigeringerPåBehandling(behandlingId: Long): List<EtterbetalingKorrigering>
}
