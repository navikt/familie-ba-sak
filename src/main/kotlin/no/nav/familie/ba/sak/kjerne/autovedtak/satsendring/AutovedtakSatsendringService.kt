package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val satskjøringRepository: SatskjøringRepository,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val satsendringService: SatsendringService
) {

    private val satsendringAlleredeUtført = Metrics.counter("satsendring.allerede.utfort")
    private val satsendringIverksatt = Metrics.counter("satsendring.iverksatt")
    private val satsendringIgnorertÅpenBehandling = Metrics.counter("satsendring.ignorert.aapenbehandling")

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    fun kjørBehandling(behandlingsdata: SatsendringTaskDto): SatsendringSvar {
        val fagsakId = behandlingsdata.fagsakId

        val satskjøringForFagsak =
            satskjøringRepository.findByFagsakId(fagsakId)
                ?: satskjøringRepository.save(Satskjøring(fagsakId = fagsakId))

        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            ?: error("Fant ikke siste iverksette behandling for $fagsakId")

        if (satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId)) {
            satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført for fagsak=$fagsakId")
            satsendringAlleredeUtført.increment()
            return SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT
        }

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteIverksatteBehandling.fagsak.id)
        val søkerAktør = sisteIverksatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteIverksatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteIverksatteBehandling for ${søkerAktør.aktivFødselsnummer()}")
        if (sisteIverksatteBehandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${sisteIverksatteBehandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingSvar = hentBrukerHarÅpenBehandlingSvar(aktivOgÅpenBehandling)

            satskjøringForFagsak.feiltype = "ÅPEN_BEHANDLING"
            satskjøringRepository.save(satskjøringForFagsak)

            logger.info(brukerHarÅpenBehandlingSvar.melding)
            satsendringIgnorertÅpenBehandling.increment()

            return brukerHarÅpenBehandlingSvar
        }

        if (harUtbetalingerSomOverstiger100Prosent(sisteIverksatteBehandling)) {
            logger.warn("Det løper over 100 prosent utbetaling på fagsak=${sisteIverksatteBehandling.fagsak.id}")
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

        val task = when (behandlingEtterBehandlingsresultat.steg) {
            StegType.IVERKSETT_MOT_OPPDRAG -> {
                IverksettMotOppdragTask.opprettTask(
                    behandlingEtterBehandlingsresultat,
                    opprettetVedtak,
                    SikkerhetContext.hentSaksbehandler()
                )
            }

            StegType.FERDIGSTILLE_BEHANDLING -> {
                FerdigstillBehandlingTask.opprettTask(
                    søkerAktør.aktivFødselsnummer(),
                    behandlingEtterBehandlingsresultat.id
                )
            }

            else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved satsendring for fagsak=$fagsakId")
        }

        satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
        satskjøringRepository.save(satskjøringForFagsak)
        taskRepository.save(task)
        satsendringIverksatt.increment()

        return SatsendringSvar.SATSENDRING_KJØRT_OK
    }

    private fun hentBrukerHarÅpenBehandlingSvar(
        aktivOgÅpenBehandling: Behandling
    ): SatsendringSvar {
        val brukerHarÅpenBehandlingSvar = if (satsendringService.erFagsakOppdatertMedSisteSatser(aktivOgÅpenBehandling.fagsak.id)) {
            SatsendringSvar.HAR_ALLEREDE_SISTE_SATS
        } else if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
            SatsendringSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG
        } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
            SatsendringSvar.TILBAKESTILLER_BEHANDLINGEN_TIL_VILKÅRSVURDERINGEN
        } else {
            SatsendringSvar.BEHANDLINGEN_ER_UNDER_UTREDNING_MEN_I_RIKTIG_TILSTAND
        }
        return brukerHarÅpenBehandlingSvar
    }

    private fun harUtbetalingerSomOverstiger100Prosent(sisteIverksatteBehandling: Behandling): Boolean {
        val tilkjentYtelse =
            beregningService.hentTilkjentYtelseForBehandling(behandlingId = sisteIverksatteBehandling.id)
        val personopplysningGrunnlag =
            persongrunnlagService.hentAktivThrows(behandlingId = sisteIverksatteBehandling.id)

        val barnMedAndreRelevanteTilkjentYtelser = personopplysningGrunnlag.barna.map {
            Pair(
                it,
                beregningService.hentRelevanteTilkjentYtelserForBarn(it.aktør, sisteIverksatteBehandling.fagsak.id)
            )
        }

        try {
            TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
                barnMedAndreRelevanteTilkjentYtelser = barnMedAndreRelevanteTilkjentYtelser,
                personopplysningGrunnlag = personopplysningGrunnlag
            )
        } catch (e: UtbetalingsikkerhetFeil) {
            secureLogger.info("fagsakId=${sisteIverksatteBehandling.fagsak.id} har UtbetalingsikkerhetFeil. Skipper satsendring: ${e.frontendFeilmelding}")
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

enum class SatsendringSvar(val melding: String) {
    SATSENDRING_KJØRT_OK(melding = "Satsendring kjørt OK"),
    SATSENDRING_ER_ALLEREDE_UTFØRT(melding = "Satsendring allerede utført for fagsak"),
    HAR_ALLEREDE_SISTE_SATS(melding = "Åpen behandling har allerede siste sats og vi lar den ligge."),
    BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG(
        melding = "Behandlingen er låst for endringer og satsendring vil bli trigget neste virkedag."
    ),
    TILBAKESTILLER_BEHANDLINGEN_TIL_VILKÅRSVURDERINGEN(melding = "Tilbakestiller behandlingen til vilkårsvurderingen"),
    BEHANDLINGEN_ER_UNDER_UTREDNING_MEN_I_RIKTIG_TILSTAND(
        melding = "Behandlingen er under utredning, men er allerede i riktig tilstand."
    )
}
