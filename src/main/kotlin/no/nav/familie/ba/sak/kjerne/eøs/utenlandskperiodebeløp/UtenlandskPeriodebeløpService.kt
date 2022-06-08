package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import org.springframework.stereotype.Service

@Service
class UtenlandskPeriodebeløpService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>
) {
    val skjemaService = PeriodeOgBarnSkjemaService(
        utenlandskPeriodebeløpRepository,
        endringsabonnenter
    )

    fun hentUtenlandskePeriodebeløp(behandlingId: BehandlingId) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: BehandlingId, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        skjemaService.slettSkjema(utenlandskPeriodebeløpId)
}
