package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class UtbetalingsoppdragGeneratorService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val beregningService: BeregningService,
    private val featureToggleService: FeatureToggleService,
) {

    fun genererUtbetalingsoppdrag(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = hentForrigeTilkjentYtelse(vedtak.behandling)
        val nyTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = vedtak.behandling.id)
        val endretMigreringsDato = beregnOmMigreringsDatoErEndret(
            vedtak.behandling,
            forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minByOrNull { it.stønadFom }?.stønadFom,
        )
        val sisteAndelPerKjede = hentSisteAndelTilkjentYtelse(vedtak.behandling.fagsak)
        val beregnetUtbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            saksbehandlerId = saksbehandlerId,
            vedtak = vedtak,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            nyTilkjentYtelse = nyTilkjentYtelse,
            sisteAndelPerKjede = sisteAndelPerKjede,
            erSimulering = erSimulering,
            endretMigreringsDato = endretMigreringsDato,
        )

        if (!erSimulering && featureToggleService.isEnabled(FeatureToggleConfig.BRUK_NY_UTBETALINGSGENERATOR, false)) {
            oppdaterTilkjentYtelse(nyTilkjentYtelse, beregnetUtbetalingsoppdrag)
        }

        return beregnetUtbetalingsoppdrag
    }

    private fun oppdaterTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
    ) {
        tilkjentYtelse.utbetalingsoppdrag =
            objectMapper.writeValueAsString(beregnetUtbetalingsoppdrag.utbetalingsoppdrag)
        val andelerPåId = beregnetUtbetalingsoppdrag.andeler.associateBy { it.id }
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
        val andelerSomSkalSendesTilOppdrag = andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
        if (beregnetUtbetalingsoppdrag.andeler.size != andelerSomSkalSendesTilOppdrag.size) {
            error("Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt antallet andeler med kalkulertUtbetalingsbeløp != 0. Generator gir ${beregnetUtbetalingsoppdrag.andeler.size} andeler men det er ${andelerSomSkalSendesTilOppdrag.size} andeler med kalkulertUtbetalingsbeløp != 0")
        }
        andelerSomSkalSendesTilOppdrag.forEach { andel ->
            val andelMedOffset = andelerPåId[andel.id]
                ?: error("Feil ved oppdaterig av offset på andeler. Finner ikke andel med id ${andel.id} blandt andelene med oppdatert offset fra ny generator. Ny generator returnerer andeler med ider [${andelerPåId.values.map { it.id }}]")
            andel.periodeOffset = andelMedOffset.periodeId
            andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
            andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
        }
        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = behandling)
            ?.let { tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }

    private fun hentSisteAndelTilkjentYtelse(fagsak: Fagsak) =
        andelTilkjentYtelseRepository.hentSisteAndelPerIdent(fagsakId = fagsak.id)
            .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
        erSimulering: Boolean = false,
        skalValideres: Boolean = true,
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand =
            beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = oppdatertBehandling.id)
                .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

        val oppdaterteKjeder = grupperAndeler(oppdatertTilstand)

        val erFørsteIverksatteBehandlingPåFagsak =
            beregningService.hentSisteAndelPerIdent(fagsakId = oppdatertBehandling.fagsak.id)
                .isEmpty()

        val utbetalingsoppdrag = if (erFørsteIverksatteBehandlingPåFagsak) {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering,
            )
        } else {
            val forrigeBehandling =
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = oppdatertBehandling)
                    ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

            val forrigeTilstand =
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)
                    .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

            val forrigeKjeder = grupperAndeler(forrigeTilstand)

            val sisteAndelPerIdent = beregningService.hentSisteAndelPerIdent(forrigeBehandling.fagsak.id)

            if (oppdatertTilstand.isNotEmpty()) {
                oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder = oppdaterteKjeder, forrigeKjeder = forrigeKjeder)
                val tilkjentYtelseMedOppdaterteAndeler = oppdatertTilstand.first().tilkjentYtelse
                beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelseMedOppdaterteAndeler)
            }

            val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                forrigeKjeder = forrigeKjeder,
                sisteAndelPerIdent = sisteAndelPerIdent,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering,
                endretMigreringsDato = beregnOmMigreringsDatoErEndret(
                    vedtak.behandling,
                    forrigeTilstand.minByOrNull { it.stønadFom }?.stønadFom,
                ),
            )

            if (!erSimulering && (
                    oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT || behandlingHentOgPersisterService.hent(
                        oppdatertBehandling.id,
                    ).resultat == Behandlingsresultat.OPPHØRT
                    )
            ) {
                utbetalingsoppdrag.validerOpphørsoppdrag()
            }

            utbetalingsoppdrag
        }

        if (skalValideres) {
            utbetalingsoppdrag.validerNullutbetaling(
                behandlingskategori = vedtak.behandling.kategori,
                andelerTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseForBehandling(vedtak.behandling.id),
            )
        }

        opprettAdvarselLoggVedForstattInnvilgetMedUtbetaling(utbetalingsoppdrag, vedtak.behandling)

        return utbetalingsoppdrag
    }

    private fun beregnOmMigreringsDatoErEndret(behandling: Behandling, forrigeTilstandFraDato: YearMonth?): YearMonth? {
        val erMigrertSak =
            behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato = behandlingService.hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
            ?.toYearMonth()
            ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null &&
            nyttTilstandFraDato != null &&
            forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)
        ) {
            nyttTilstandFraDato
        } else {
            null
        }
    }
}
