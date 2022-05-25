package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkjemaendringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val valutakursService: ValutakursService
) {
    @Transactional
    fun kompetanserEndret(behandlingId: Long) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
        utenlandskPeriodebeløpService.tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId)
    }

    @Transactional
    fun utenlandskePeriodebeløpEndret(behandlingId: Long) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
        valutakursService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)
    }

    @Transactional
    fun valutakurserEndret(behandlingId: Long) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}
