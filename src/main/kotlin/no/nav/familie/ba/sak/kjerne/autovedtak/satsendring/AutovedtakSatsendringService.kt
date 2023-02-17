package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringSvar.ÅpenBehandlingSvar
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
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
import java.time.YearMonth

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val andelTilkjentYtelseMedEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val satskjøringRepository: SatskjøringRepository,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService
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
            satskjøringRepository.findByFagsakId(fagsakId) ?: error("Fant ingen satskjøringsrad for fagsak=$fagsakId")

        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            ?: error("Fant ikke siste iverksette behandling for $fagsakId")

        if (harAlleredeNySats(sisteIverksatteBehandling.id, behandlingsdata.satstidspunkt)) {
            satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført for fagsak=$fagsakId")
            satsendringAlleredeUtført.increment()
            return SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT(fagsakId)
        }

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteIverksatteBehandling.fagsak.id)
        val søkerAktør = sisteIverksatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteIverksatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteIverksatteBehandling for ${søkerAktør.aktivFødselsnummer()}")
        if (sisteIverksatteBehandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${sisteIverksatteBehandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingSvar = hentBrukerHarÅpenBehandlingSvar(aktivOgÅpenBehandling, behandlingsdata)

            satskjøringForFagsak.feiltype = "ÅPEN_BEHANDLING"
            satskjøringRepository.save(satskjøringForFagsak)

            logger.info(brukerHarÅpenBehandlingSvar.melding)
            satsendringIgnorertÅpenBehandling.increment()

            return brukerHarÅpenBehandlingSvar
        }

        if (harUtbetalingerSomOverstiger100Prosent(sisteIverksatteBehandling)) {
            satskjøringForFagsak.feiltype = "UTBETALING_OVER_100_PROSENT"
            satskjøringRepository.save(satskjøringForFagsak)
            return SatsendringSvar.FANT_OVER_100_PROSENT_UTBETALING
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
                behandlingService.oppdaterStatusPåBehandling(
                    behandlingEtterBehandlingsresultat.id,
                    BehandlingStatus.IVERKSETTER_VEDTAK
                )
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
        aktivOgÅpenBehandling: Behandling,
        behandlingsdata: SatsendringTaskDto
    ): ÅpenBehandlingSvar {
        val brukerHarÅpenBehandlingSvar = if (harAlleredeNySats(
                sisteIverksettBehandlingsId = aktivOgÅpenBehandling.id,
                satstidspunkt = behandlingsdata.satstidspunkt
            )
        ) {
            ÅpenBehandlingSvar.HAR_ALLEREDE_SISTE_SATS
        } else if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
            ÅpenBehandlingSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG(aktivOgÅpenBehandling)
        } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
            ÅpenBehandlingSvar.TILBAKESTILLER_BEHANDLINGEN_TIL_VILKÅRSVURDERINGEN(aktivOgÅpenBehandling)
        } else {
            ÅpenBehandlingSvar.BEHANDLINGEN_ER_UNDER_UTREDNING_MEN_I_RIKTIG_TILSTAND(
                aktivOgÅpenBehandling
            )
        }
        return brukerHarÅpenBehandlingSvar
    }

    fun harAlleredeNySats(sisteIverksettBehandlingsId: Long, satstidspunkt: YearMonth): Boolean {
        val andeler =
            andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                sisteIverksettBehandlingsId
            )

        return harAlleredeSisteSats(andeler, satstidspunkt)
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

sealed interface SatsendringSvar {
    val melding: String

    object FANT_OVER_100_PROSENT_UTBETALING : SatsendringSvar {
        override val melding = "Fant utbetaling over 100 prosent på barna"
    }

    object SATSENDRING_KJØRT_OK : SatsendringSvar {
        override val melding = "Satsendring kjørt OK"
    }

    class SATSENDRING_ER_ALLEREDE_UTFØRT(fagsakId: Long) : SatsendringSvar {
        override val melding = "Satsendring allerede utført for fagsak=$fagsakId"
    }

    sealed interface ÅpenBehandlingSvar : SatsendringSvar {
        object HAR_ALLEREDE_SISTE_SATS : ÅpenBehandlingSvar {
            override val melding = "Åpen behandling har allerede siste sats og vi lar den ligge."
        }

        class BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG(aktivOgÅpenBehandling: Behandling) :
            ÅpenBehandlingSvar {
            override val melding =
                "Behandling $aktivOgÅpenBehandling er låst for endringer og satsendring vil bli trigget neste virkedag."
        }

        class TILBAKESTILLER_BEHANDLINGEN_TIL_VILKÅRSVURDERINGEN(aktivOgÅpenBehandling: Behandling) :
            ÅpenBehandlingSvar {
            override val melding = "Tilbakestiller behandling $aktivOgÅpenBehandling til vilkårsvurderingen"
        }

        class BEHANDLINGEN_ER_UNDER_UTREDNING_MEN_I_RIKTIG_TILSTAND(aktivOgÅpenBehandling: Behandling) :
            ÅpenBehandlingSvar {
            override val melding =
                "Behandling $aktivOgÅpenBehandling er under utredning, men er allerede i riktig tilstand."
        }
    }
}
