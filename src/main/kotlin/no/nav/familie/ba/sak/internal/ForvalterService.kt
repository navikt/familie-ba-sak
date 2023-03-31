package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
) {

    @Transactional
    fun lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(behandlingId: Long){
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)

        if (tilkjentYtelse.utbetalingsoppdrag != null){
            throw Feil("Behandling $behandlingId har allerede opprettet utbetalingsoppdrag")
        }

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtak,
            saksbehandlerId = "VL",
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForIverksettingFactory()
        )
    }
}