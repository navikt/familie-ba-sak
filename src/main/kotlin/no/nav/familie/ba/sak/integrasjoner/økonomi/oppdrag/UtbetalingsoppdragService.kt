package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class UtbetalingsoppdragService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        behandlingId: Long,
        vedtak: Vedtak,
        forrigeBehandlingId: Long?,
        erSimulering: Boolean,
        endretMigreringsdato: YearMonth?
    ): Utbetalingsoppdrag {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        val behandlingsinformasjon = Behandlingsinformasjon(
            saksbehandlerId = vedtak.endretAv,
            behandlingId = behandlingId,
            fagsakId = vedtak.behandling.fagsak.id,
            aktør = vedtak.behandling.fagsak.aktør,
            vedtak = vedtak,
            erSimulering = erSimulering,
            endretMigreringsDato = endretMigreringsdato,
        )
        val sisteAndelPerKjede = sisteAndelPerKjedeForFagsak(behandlingsinformasjon)
        val resultat = NyUtbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            behandlingsinformasjon,
            tilkjentYtelse.andelerTilkjentYtelse.map { AndelData(it) },
            forrigeAndeler(forrigeBehandlingId),
            sisteAndelPerKjede,
        )
        if (!erSimulering) {
            oppdaterTilkjentYtelse(tilkjentYtelse, resultat)
        }
        return resultat.utbetalingsoppdrag
    }

    private fun oppdaterTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        resultat: UtbetalingsoppdragOgAndelerMedOffset,
    ) {
        tilkjentYtelse.utbetalingsoppdrag = objectMapper.writeValueAsString(resultat.utbetalingsoppdrag)
        val andelerPåId = resultat.andeler.associateBy { it.id }
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
        val andelerSomSkalSendesTilOppdrag = andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
        if (resultat.andeler.size != andelerSomSkalSendesTilOppdrag.size) {
            error("Forventer å ha like mange")
        }
        andelerSomSkalSendesTilOppdrag.forEach { andel ->
            val andelMedOffset = andelerPåId[andel.id]
                ?: error("Finner ikke andel med offset for andel=${andel.id}")
            andel.periodeOffset = andelMedOffset.offset
            andel.forrigePeriodeOffset = andelMedOffset.forrigeOffset
            andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
        }
        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    private fun sisteAndelPerKjedeForFagsak(behandlingsinformasjon: Behandlingsinformasjon) =
        tilkjentYtelseRepository.sisteAndelPerKjedeForFagsak(behandlingsinformasjon.fagsakId)
            .mapValues { AndelData(it.value) }

    private fun forrigeAndeler(forrigeBehandlingId: Long?): List<AndelData>? =
        forrigeBehandlingId?.let {
            tilkjentYtelseRepository.findByBehandling(it)
        }?.andelerTilkjentYtelse?.map { AndelData(it) }
}
