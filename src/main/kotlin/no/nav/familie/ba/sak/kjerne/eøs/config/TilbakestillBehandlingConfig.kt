package no.nav.familie.ba.sak.kjerne.eøs.config

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingPgaKompetanseEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    override fun skjemaerEndret(behandlingId: Long, endretTil: Collection<Kompetanse>) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}

@Service
class TilbakestillBehandlingPgaUtenlandskPeriodebeløpEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(behandlingId: Long, endretTil: Collection<UtenlandskPeriodebeløp>) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}

@Service
class TilbakestillBehandlingPgaValutakursEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(behandlingId: Long, endretTil: Collection<Valutakurs>) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}
