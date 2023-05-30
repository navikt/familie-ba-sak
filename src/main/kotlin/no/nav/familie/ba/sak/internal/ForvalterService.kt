package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {

    @Transactional
    fun lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandlingSendtTilØkonomi =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        val erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi = forrigeBehandlingSendtTilØkonomi != null &&
            forrigeBehandlingSendtTilØkonomi.aktivertTidspunkt.isAfter(behandling.aktivertTidspunkt)

        if (tilkjentYtelse.utbetalingsoppdrag != null) {
            throw Feil("Behandling $behandlingId har allerede opprettet utbetalingsoppdrag")
        }
        if (erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi) {
            throw Feil("Det finnes en behandling opprettet etter $behandlingId som er sendt til økonomi")
        }

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId),
            saksbehandlerId = "VL",
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForIverksettingFactory(),
        )
    }
}
