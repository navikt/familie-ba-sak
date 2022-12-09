package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TrekkILøpendeUtbetalingRepository : JpaRepository<TrekkILøpendeUtbetaling, Long>{
    @Query(value = "SELECT t FROM TrekkILøpendeUtbetaling t WHERE t.behandlingId = :behandlingId ORDER BY t.opprettetTidspunkt ASC")
    fun finnTrekkILøpendeUtbetalingForBehandling(behandlingId: Long): List<TrekkILøpendeUtbetaling>
}
