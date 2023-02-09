package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val andelTilkjentYtelseMedEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val satskjøringRepository: SatskjøringRepository
) : AutovedtakBehandlingService<SatsendringTaskDto> {

    private val satsendringAlleredeUtført = Metrics.counter("satsendring.allerede.utfort")
    private val satsendringIverksatt = Metrics.counter("satsendring.iverksatt")
    private val satsendringIgnorertÅpenBehandling = Metrics.counter("satsendring.ignorert.aapenbehandling")

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    override fun kjørBehandling(behandlingsdata: SatsendringTaskDto): String {
        val fagsakId = behandlingsdata.fagsakId
        val satskjøringForFagsak =
            satskjøringRepository.findByFagsakId(fagsakId) ?: error("Fant ingen satskjøringsrad for fagsak=$fagsakId")

        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            ?: error("Fant ikke siste iverksette behandling for $fagsakId")

        if (harAlleredeNySats(sisteIverksatteBehandling.id, behandlingsdata.satstidspunkt)) {
            satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført for fagsak=$fagsakId")
            satsendringAlleredeUtført.increment()
            return "Satsendring allerede utført for fagsak=$fagsakId"
        }

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteIverksatteBehandling.fagsak.id)
        val søkerAktør = sisteIverksatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteIverksatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteIverksatteBehandling for ${søkerAktør.aktivFødselsnummer()}")
        if (sisteIverksatteBehandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${sisteIverksatteBehandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingMelding = if (harAlleredeNySats(
                    sisteIverksettBehandlingsId = aktivOgÅpenBehandling.id,
                    satstidspunkt = behandlingsdata.satstidspunkt
                )
            ) {
                "Åpen behandling har allerede siste sats og vi lar den ligge."
            } else if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
                "Behandling $aktivOgÅpenBehandling er låst for endringer og satsendring vil bli trigget neste virkedag."
            } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
                tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
                "Tilbakestiller behandling $aktivOgÅpenBehandling til vilkårsvurderingen"
            } else {
                "Behandling $aktivOgÅpenBehandling er under utredning, men er allerede i riktig tilstand."
            }

            logger.info(brukerHarÅpenBehandlingMelding)
            satsendringIgnorertÅpenBehandling.increment()
            return brukerHarÅpenBehandlingMelding
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                fagsakId = sisteIverksatteBehandling.fagsak.id
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        val task = IverksettMotOppdragTask.opprettTask(
            behandlingEtterBehandlingsresultat,
            opprettetVedtak,
            SikkerhetContext.hentSaksbehandler()
        )

        satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
        satskjøringRepository.save(satskjøringForFagsak)
        taskRepository.save(task)
        satsendringIverksatt.increment()

        return "Satsendring kjørt OK"
    }

    fun harAlleredeNySats(sisteIverksettBehandlingsId: Long, satstidspunkt: YearMonth): Boolean {
        val andeler =
            andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                sisteIverksettBehandlingsId
            )

        return harAlleredeSisteSats(andeler, satstidspunkt)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun harAlleredeSisteSats(
            aty: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
            satstidspunkt: YearMonth
        ): Boolean {
            val atyPåSatstidspunkt = aty.filter {
                it.stønadFom.isSameOrBefore(satstidspunkt) && it.stønadTom.isSameOrAfter(
                    satstidspunkt
                )
            }

            val atySmåbarnstillegg = atyPåSatstidspunkt.filter { it.type == YtelseType.SMÅBARNSTILLEGG }
            if (atySmåbarnstillegg.isNotEmpty()) {
                val harGammelSats =
                    atySmåbarnstillegg.any { it.sats != SatsService.finnSisteSatsFor(SatsType.SMA).beløp }
                if (harGammelSats) return false
            }

            val atyUtvidet = atyPåSatstidspunkt.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
            if (atyUtvidet.isNotEmpty()) {
                val harGammelSats =
                    atyUtvidet.any { it.sats != SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp }
                if (harGammelSats) return false
            }

            val atyOrdinær = atyPåSatstidspunkt.filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }
            if (atyOrdinær.isNotEmpty()) {
                val satser = atyOrdinær.map { it.sats }
                val gyldigeOrdinæreSatser = listOf(
                    SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
                    SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp
                )
                satser.forEach { if (!gyldigeOrdinæreSatser.contains(it)) return false }
            }
            return true
        }
    }
}
