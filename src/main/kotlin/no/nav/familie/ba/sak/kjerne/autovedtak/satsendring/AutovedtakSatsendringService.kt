package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.SatsendringFeil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.VilkårFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutovedtakSatsendringService(
    private val satskjøringRepository: SatskjøringRepository,
    private val satsendringService: SatsendringService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakSatsendringRollbackService: AutovedtakSatsendringRollbackService,
) {
    private val satsendringAlleredeUtført = Metrics.counter("satsendring.allerede.utfort")
    private val satsendringIverksatt = Metrics.counter("satsendring.iverksatt")

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    fun kjørBehandling(behandlingsdata: SatsendringTaskDto): SatsendringSvar {
        val fagsakId = behandlingsdata.fagsakId

        val satskjøringForFagsak =
            satskjøringRepository.findByFagsakIdAndSatsTidspunkt(fagsakId, behandlingsdata.satstidspunkt)
                ?: satskjøringRepository.save(Satskjøring(fagsakId = fagsakId, satsTidspunkt = behandlingsdata.satstidspunkt))

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId) ?: error("Fant ikke siste vedtatte behandling for $fagsakId")
        if (sisteVedtatteBehandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${sisteVedtatteBehandling.fagsak.id}")

        if (satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId)) {
            satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført for fagsak=$fagsakId")
            satsendringAlleredeUtført.increment()
            return SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT
        }

        if (harUtbetalingerSomOverstiger100Prosent(sisteVedtatteBehandling)) {
            logger.warn("Det løper over 100 prosent utbetaling på fagsak=${sisteVedtatteBehandling.fagsak.id}")
        }

        try {
            autovedtakSatsendringRollbackService.kjørSatsendring(fagsakId)
        } catch (e: VilkårFeil) {
            return utledSatsendringSvar(satskjøringForFagsak, SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_VILKÅR)
        } catch (e: SatsendringFeil) {
            logger.error(e.message)
            return utledSatsendringSvar(satskjøringForFagsak, e.satsendringSvar)
        }

        satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
        satskjøringRepository.save(satskjøringForFagsak)

        satsendringIverksatt.increment()

        return SatsendringSvar.SATSENDRING_KJØRT_OK
    }

    private fun utledSatsendringSvar(
        satskjøringForFagsak: Satskjøring,
        satsendringSvar: SatsendringSvar,
    ): SatsendringSvar {
        satskjøringForFagsak.feiltype = satsendringSvar.name
        satskjøringRepository.save(satskjøringForFagsak)
        logger.warn(satsendringSvar.melding)

        return satsendringSvar
    }

    private fun harUtbetalingerSomOverstiger100Prosent(sisteIverksatteBehandling: Behandling): Boolean {
        try {
            tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(sisteIverksatteBehandling)
        } catch (e: UtbetalingsikkerhetFeil) {
            secureLogger.info("fagsakId=${sisteIverksatteBehandling.fagsak.id} har UtbetalingsikkerhetFeil. Skipper satsendring: ${e.frontendFeilmelding}")
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
    }
}

enum class SatsendringSvar(
    val melding: String,
) {
    SATSENDRING_KJØRT_OK(melding = "Satsendring kjørt OK"),
    SATSENDRING_ER_ALLEREDE_UTFØRT(melding = "Satsendring allerede utført for fagsak"),
    BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG(
        melding = "Behandlingen er låst for endringer og satsendring vil bli trigget neste virkedag.",
    ),
    BEHANDLING_KAN_SNIKES_FORBI("Behandling kan snikes forbi (toggle er slått av)"),
    BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT("Behandlingen kan ikke settes på vent"),
    BEHANDLING_HAR_FEIL_PÅ_VILKÅR("Behandlingen feiler på validering av vilkår."),
    BEHANDLING_HAR_FEIL_PÅ_ANDELER("Behandlingen feiler på validering av andeler."),
}
