package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import org.springframework.data.jpa.repository.Query

interface UtenlandskPeriodebeløpRepository : PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> {
    @Query("SELECT upb FROM UtenlandskPeriodebeløp upb WHERE upb.behandlingId = :behandlingId")
    override fun finnFraBehandlingId(behandlingId: Long): Collection<UtenlandskPeriodebeløp>

    @Query("SELECT upb FROM UtenlandskPeriodebeløp  upb WHERE (upb.utbetalingsland = 'NO' or upb.utbetalingsland is null)")
    fun hentUtenlandskePeriodebeløpMedFeilUtbetalingsland(): Collection<UtenlandskPeriodebeløp>
}
