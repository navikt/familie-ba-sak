package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.OppdaterTilkjentYtelseService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.tilUtbetalingsoppdragDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ØkonomiService(
    private val økonomiKlient: ØkonomiKlient,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val oppdaterTilkjentYtelseService: OppdaterTilkjentYtelseService,
) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ba.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String,
    ): Utbetalingsoppdrag {
        val behandling = vedtak.behandling

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                tilkjentYtelse = tilkjentYtelse,
            )

        oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
            tilkjentYtelse = tilkjentYtelse,
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
        )

        val utbetalingsoppdrag =
            beregnetUtbetalingsoppdrag
                .utbetalingsoppdrag
                .tilUtbetalingsoppdragDto()

        tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId = behandling.id)

        iverksettOppdrag(utbetalingsoppdrag, behandling.id)

        return utbetalingsoppdrag
    }

    private fun iverksettOppdrag(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        behandlingId: Long,
    ) {
        if (!utbetalingsoppdrag.skalIverksettesMotOppdrag()) {
            logger.warn(
                "Iverksetter ikke noe mot oppdrag. " +
                    "Ingen utbetalingsperioder for behandlingId=$behandlingId",
            )
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

    fun hentStatus(
        oppdragId: OppdragId,
        behandlingId: Long,
    ): OppdragStatus =
        if (tilkjentYtelseRepository.findByBehandling(behandlingId).skalIverksettesMotOppdrag()) {
            økonomiKlient.hentStatus(oppdragId)
        } else {
            OppdragStatus.KVITTERT_OK
        }

    fun opprettManuellKvitteringPåOppdrag(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val oppdragId =
            OppdragId(
                fagsystem = FAGSYSTEM,
                personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingsId = behandling.id.toString(),
            )

        økonomiKlient.opprettManuellKvitteringPåOppdrag(oppdragId)
    }

    companion object {
        val logger = LoggerFactory.getLogger(ØkonomiService::class.java)
    }
}

fun Utbetalingsoppdrag.skalIverksettesMotOppdrag(): Boolean = utbetalingsperiode.isNotEmpty()

private fun TilkjentYtelse.skalIverksettesMotOppdrag(): Boolean = this.utbetalingsoppdrag()?.skalIverksettesMotOppdrag() ?: false
