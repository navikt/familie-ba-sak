package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
interface SatskjøringRepository : JpaRepository<Satskjøring, Long> {
    fun countByFerdigTidspunktIsNotNullAndSatsTidspunkt(satsTidspunkt: YearMonth): Long

    fun countBySatsTidspunkt(satsTidspunkt: YearMonth): Long

    fun findByFagsakIdAndSatsTidspunkt(
        fagsakId: Long,
        satsTidspunkt: YearMonth,
    ): Satskjøring?

    @Query(value = "SELECT sk from Satskjøring sk where sk.ferdigTidspunkt IS NULL and sk.feiltype = :feiltype and sk.satsTidspunkt = :satsTidspunkt")
    fun finnPåFeilTypeOgFerdigTidNull(
        feiltype: String,
        satsTidspunkt: YearMonth,
    ): List<Satskjøring>

    fun findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(satsTidspunkt: YearMonth): List<Satskjøring>
}
