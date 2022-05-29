package no.nav.familie.ba.sak.kjerne.eøs.config

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilbakestillBehandlingÉnGangService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingService
) {
    var gjeldendeKorrelasjonId: UUID? = null

    fun tilbakeStillBehandling(behandlingId: BehandlingId) {
        if (behandlingId.korrelasjonsid != gjeldendeKorrelasjonId) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
            gjeldendeKorrelasjonId = behandlingId.korrelasjonsid
        }
    }
}

@Service
class TilbakestillBehandlingPgaKompetanseEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingÉnGangService
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Kompetanse>) {
        tilbakestillBehandlingService.tilbakeStillBehandling(behandlingId)
    }
}

@Service
class TilbakestillBehandlingPgaUtenlandskPeriodebeløpEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingÉnGangService
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<UtenlandskPeriodebeløp>) {
        tilbakestillBehandlingService.tilbakeStillBehandling(behandlingId)
    }
}

@Service
class TilbakestillBehandlingPgaValutakursEndringService(
    private val tilbakestillBehandlingService: TilbakestillBehandlingÉnGangService
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Valutakurs>) {
        tilbakestillBehandlingService.tilbakeStillBehandling(behandlingId)
    }
}
