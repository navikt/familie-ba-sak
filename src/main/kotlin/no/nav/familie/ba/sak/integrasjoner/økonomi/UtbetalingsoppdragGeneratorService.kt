package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UtbetalingsoppdragGeneratorService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val oppdaterTilkjentYtelseService: OppdaterTilkjentYtelseService,
) {
    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val nyTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = vedtak.behandling.id)
        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                nyTilkjentYtelse = nyTilkjentYtelse,
                erSimulering = erSimulering,
            )
        if (!erSimulering) {
            oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                tilkjentYtelse = nyTilkjentYtelse,
                beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            )
        }
        return beregnetUtbetalingsoppdrag
    }
}
