package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
interface SatsendringEøsKjøringRepository : JpaRepository<SatsendringEøsKjøring, Long> {
    fun findByBehandlingId(behandlingId: Long): SatsendringEøsKjøring?

    fun findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
    ): SatsendringEøsKjøring?
}
