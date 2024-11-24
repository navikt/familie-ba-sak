package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import org.springframework.stereotype.Service

@Service
class UtbetalingerPerMndEøsService(
    private val valutakursRepository: ValutakursRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val starttidspunktForUtbetalingstabellService: StarttidspunktForUtbetalingstabellService,
) {
    fun hentUtbetalingerPerMndEøs(
        vedtak: Vedtak,
    ): Map<String, UtbetalingMndEøs>? {
        val behandlingId = vedtak.behandling.id
        val endringstidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling = vedtak.behandling)
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId = behandlingId)
        val endretutbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandlingId)

        if (!skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser)) {
            return null
        }

        val andelerTilkjentYtelseForBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandlingId).toList()

        return hentUtbetalingerPerMndEøs(
            endringstidspunkt = endringstidspunkt,
            andelTilkjentYtelserForBehandling = andelerTilkjentYtelseForBehandling,
            utenlandskePeriodebeløp = utenlandskePeriodebeløp,
            valutakurser = valutakurser,
            endretutbetalingAndeler = endretutbetalingAndeler,
        )
    }
}
