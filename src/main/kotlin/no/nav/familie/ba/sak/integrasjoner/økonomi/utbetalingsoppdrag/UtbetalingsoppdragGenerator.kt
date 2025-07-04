package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component

@Component
class UtbetalingsoppdragGenerator(
    private val utbetalingsgenerator: Utbetalingsgenerator,
    private val klassifiseringKorrigerer: KlassifiseringKorrigerer,
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = hentForrigeTilkjentYtelse(vedtak.behandling)
        val sisteAndelPerKjede = hentSisteAndelTilkjentYtelse(vedtak.behandling)

        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId,
                vedtak,
                forrigeTilkjentYtelse,
                sisteAndelPerKjede,
                erSimulering,
            )

        val forrigeAndeler =
            forrigeTilkjentYtelse?.tilAndelData() ?: emptyList()

        val nyeAndeler = tilkjentYtelse.tilAndelData()

        val beregnetUtbetalingsoppdrag =
            utbetalingsgenerator.lagUtbetalingsoppdrag(
                behandlingsinformasjon = behandlingsinformasjon,
                forrigeAndeler = forrigeAndeler,
                nyeAndeler = nyeAndeler,
                sisteAndelPerKjede = sisteAndelPerKjede,
            )

        return klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            behandling = vedtak.behandling,
        )
    }

    private fun hentSisteAndelTilkjentYtelse(
        behandling: Behandling,
    ): Map<IdentOgType, AndelDataLongId> {
        val sisteAndelPerKjede =
            andelTilkjentYtelseRepository
                .hentSisteAndelPerIdentOgType(fagsakId = behandling.fagsak.id)
                .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }

        return sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId() }
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingHentOgPersisterService
            .hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }
}

fun TilkjentYtelse.tilAndelData(): List<AndelDataLongId> = this.andelerTilkjentYtelse.map { it.tilAndelDataLongId() }

fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId =
    AndelDataLongId(
        id = id,
        fom = periode.fom,
        tom = periode.tom,
        beløp = kalkulertUtbetalingsbeløp,
        personIdent = aktør.aktivFødselsnummer(),
        type = type.tilYtelseType(),
        periodeId = periodeOffset,
        forrigePeriodeId = forrigePeriodeOffset,
        kildeBehandlingId = kildeBehandlingId,
    )
