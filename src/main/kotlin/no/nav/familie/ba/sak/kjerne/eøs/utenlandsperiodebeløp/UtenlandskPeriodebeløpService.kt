package no.nav.familie.ba.sak.kjerne.eøs.utenlandsperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service

@Service
class UtenlandskPeriodebeløpService(
    repository: UtenlandskPeriodebeløpRepository,
    tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(
        repository,
        tilbakestillBehandlingService
    )

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: Long, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        serviceDelegate.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        serviceDelegate.slettSkjema(utenlandskPeriodebeløpId)
}
