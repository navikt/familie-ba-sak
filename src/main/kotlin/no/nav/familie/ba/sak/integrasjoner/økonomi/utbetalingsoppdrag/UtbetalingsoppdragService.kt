package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class UtbetalingsoppdragService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val utbetalingsoppdragGenerator: NyUtbetalingsoppdragGenerator,
    private val behandlingService: BehandlingService,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>
) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ba.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String
    ): TilkjentYtelse {
        val oppdatertBehandling = vedtak.behandling
        val tilkjentYtelse = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(vedtak, saksbehandlerId)

        // beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse)

        // beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
        // iverksettOppdrag(utbetalingsoppdrag, oppdatertBehandling.id)

        return tilkjentYtelse
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, behandlingId: Long) {
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) {
            logger.warn("Iverksetter ikke noe mot oppdrag. Ingen utbetalingsperioder. behandlingId=$behandlingId")
            return
        }
        try {
            økonomiKlient.iverksettOppdrag(utbetalingsoppdrag)
        } catch (exception: Exception) {
            if (exception is RessursException &&
                exception.httpStatus == HttpStatus.CONFLICT
            ) {
                sammeOppdragSendtKonflikt.increment()
                logger.info("Bypasset feil med HttpKode 409 ved iverksetting mot økonomi for fagsak ${utbetalingsoppdrag.saksnummer}")
                return
            } else {
                throw exception
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus =
        økonomiKlient.hentStatus(oppdragId)

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false
    ): TilkjentYtelse {
        val behandlingId = vedtak.behandling.id
        val behandling = vedtak.behandling
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)

        // Henter tilkjentYtelse som har utbetalingsoppdrag og var sendt til oppdrag fra forrige iverksatt behandling
        val forrigeTilkjentYtelser =
            beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(behandling.fagsak.id)
        val forrigeAndeler = forrigeTilkjentYtelser.flatMap { it.andelerTilkjentYtelse }

        val endretMigreringsdato =
            beregnOmMigreringsDatoErEndret(behandling, forrigeAndeler.minByOrNull { it.stønadFom }?.stønadFom)
        val kompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId)

        val tilkjentYtelseMetaData = TilkjentYtelseMetaData(
            tilkjentYtelse = tilkjentYtelse,
            vedtak = vedtak,
            saksbehandlerId = saksbehandlerId,
            erSimulering = erSimulering,
            endretMigreringsdato = endretMigreringsdato,
            kompetanser = kompetanser.toList()
        )

        return utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            tilkjentYtelseMetaData = tilkjentYtelseMetaData,
            forrigeTilkjentYtelser = forrigeTilkjentYtelser
        )
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

    companion object {

        val logger = LoggerFactory.getLogger(UtbetalingsoppdragService::class.java)
    }
}

fun Utbetalingsoppdrag.harLøpendeUtbetaling() =
    this.utbetalingsperiode.any {
        it.opphør == null &&
            it.sats > BigDecimal.ZERO &&
            it.vedtakdatoTom > LocalDate.now().sisteDagIMåned()
    }
