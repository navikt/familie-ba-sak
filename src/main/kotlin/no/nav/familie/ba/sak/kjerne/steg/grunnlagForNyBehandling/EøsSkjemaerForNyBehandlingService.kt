package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import org.springframework.stereotype.Service

@Service
class EøsSkjemaerForNyBehandlingService(
    private val featureToggleService: FeatureToggleService,
    private val kompetanseService: KompetanseService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val valutakursService: ValutakursService
) {

    fun kopierEøsSkjemaer(behandling: Behandling, forrigeBehandlingSomErVedtatt: Behandling?) {
        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS) && forrigeBehandlingSomErVedtatt != null) {
            kompetanseService.kopierOgErstattKompetanser(
                fraBehandlingId = forrigeBehandlingSomErVedtatt.id,
                tilBehandlingId = behandling.id
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                fraBehandlingId = forrigeBehandlingSomErVedtatt.id,
                tilBehandlingId = behandling.id
            )
            valutakursService.kopierOgErstattValutakurser(
                fraBehandlingId = forrigeBehandlingSomErVedtatt.id,
                tilBehandlingId = behandling.id
            )
        }
    }
}
