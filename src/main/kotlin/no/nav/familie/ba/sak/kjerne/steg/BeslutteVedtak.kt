package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerValidering
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningBrevService
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregningService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.FerdigstillOppgaver
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.JournalførTilbakekrevingsvedtakMotregningBrevTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.ba.sak.task.PubliserVedtakV2Task
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class BeslutteVedtak(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val taskRepository: TaskRepositoryWrapper,
    private val loggService: LoggService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val featureToggleService: FeatureToggleService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val automatiskBeslutningService: AutomatiskBeslutningService,
    private val simuleringService: SimuleringService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val brevmottakerService: BrevmottakerService,
    private val tilbakekrevingsvedtakMotregningService: TilbakekrevingsvedtakMotregningService,
    private val tilbakekrevingsvedtakMotregningBrevService: TilbakekrevingsvedtakMotregningBrevService,
) : BehandlingSteg<BeslutningPåVedtakDto> {
    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            throw FunksjonellFeil("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        }
        if (behandling.status == BehandlingStatus.AVSLUTTET) {
            throw FunksjonellFeil("Behandlingen er allerede avsluttet")
        }
        if (behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV && !featureToggleService.isEnabled(FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV, behandling.id)) {
            throw FunksjonellFeil(
                melding = "Årsak ${BehandlingÅrsak.KORREKSJON_VEDTAKSBREV.visningsnavn} og toggle ${FeatureToggle.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV.navn} false",
                frontendFeilmelding = "Du har ikke tilgang til å beslutte for denne behandlingen. Ta kontakt med teamet dersom dette ikke stemmer.",
            )
        }
        if (behandling.erTekniskEndring() &&
            !featureToggleService.isEnabled(
                FeatureToggle.TEKNISK_ENDRING,
                behandling.id,
            )
        ) {
            throw FunksjonellFeil(
                "Du har ikke tilgang til å beslutte en behandling med årsak=${behandling.opprettetÅrsak.visningsnavn}. Ta kontakt med teamet dersom dette ikke stemmer.",
            )
        }
    }

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: BeslutningPåVedtakDto,
    ): StegType {
        validerBrevmottakere(BehandlingId(behandling.id), data.beslutning.erGodkjent())

        val behandlingSkalAutomatiskBesluttes = automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)

        val totrinnskontroll =
            totrinnskontrollService.besluttTotrinnskontroll(
                behandling = behandling,
                beslutter = bestemBeslutter(behandlingSkalAutomatiskBesluttes),
                beslutterId = bestemBeslutterId(behandlingSkalAutomatiskBesluttes),
                beslutning = data.beslutning,
                kontrollerteSider = data.kontrollerteSider,
            )

        opprettTaskFerdigstillGodkjenneVedtak(
            behandling = behandling,
            beslutning = data,
            behandlingErAutomatiskBesluttet = behandlingSkalAutomatiskBesluttes,
        )

        return if (data.beslutning.erGodkjent()) {
            håndterGodkjentVedtak(behandling)
        } else {
            håndterUnderkjentVedtak(behandling = behandling, totrinnskontroll = totrinnskontroll)
        }
    }

    override fun postValiderSteg(behandling: Behandling) {
        tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)
    }

    override fun stegType(): StegType = StegType.BESLUTTE_VEDTAK

    private fun bestemBeslutter(behandlingSkalAutomatiskBesluttes: Boolean): String = if (behandlingSkalAutomatiskBesluttes) SikkerhetContext.SYSTEM_NAVN else saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()

    private fun bestemBeslutterId(behandlingSkalAutomatiskBesluttes: Boolean): String = if (behandlingSkalAutomatiskBesluttes) SikkerhetContext.SYSTEM_FORKORTELSE else SikkerhetContext.hentSaksbehandler()

    private fun håndterGodkjentVedtak(behandling: Behandling): StegType {
        val erÅpenTilbakekrevingPåFagsak = tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)
        if (!erÅpenTilbakekrevingPåFagsak) {
            validerErTilbakekrevingHvisFeilutbetaling(behandling.id)
        }

        val vedtak =
            vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        tilbakekrevingsvedtakMotregningService.finnTilbakekrevingsvedtakMotregning(behandling.id)?.let {
            opprettJournalførTilbakekrevingsvedtakMotregningBrevTask(behandling)
            tilbakekrevingsvedtakMotregningBrevService.opprettOgLagreTilbakekrevingsvedtakMotregningPdf(behandling.id)
        }

        vedtakService.oppdaterVedtaksdatoOgBrev(vedtak)

        val nesteSteg = sjekkOmBehandlingSkalIverksettesOgHentNesteSteg(behandling)

        when (nesteSteg) {
            StegType.IVERKSETT_MOT_OPPDRAG -> {
                opprettTaskIverksettMotOppdrag(behandling, vedtak)
            }

            StegType.JOURNALFØR_VEDTAKSBREV -> {
                if (!behandling.erBehandlingMedVedtaksbrevutsending()) {
                    throw Feil("Prøvde å opprette vedtaksbrev for behandling som ikke skal sende ut vedtaksbrev.")
                }

                publiserStønadsstatistikkVedFortsattInnvilgetEØS(behandling)

                opprettJournalførVedtaksbrevTask(behandling, vedtak)
            }

            StegType.FERDIGSTILLE_BEHANDLING -> {
                if (behandling.type == BehandlingType.TEKNISK_ENDRING || behandling.erManuellMigreringForEndreMigreringsdato()) {
                    opprettFerdigstillBehandlingTask(behandling)
                } else {
                    throw Feil("Neste steg 'ferdigstille behandling' er ikke implementert på 'beslutte vedtak'-steg")
                }
            }

            else -> {
                throw Feil("Neste steg '$nesteSteg' er ikke implementert på 'beslutte vedtak'-steg")
            }
        }
        return nesteSteg
    }

    private fun håndterUnderkjentVedtak(
        behandling: Behandling,
        totrinnskontroll: Totrinnskontroll,
    ): StegType {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id) ?: throw Feil("Fant ikke vilkårsvurdering på behandling")
        val kopiertVilkårsVurdering = vilkårsvurdering.kopier(inkluderAndreVurderinger = true)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertVilkårsVurdering)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(
            behandling = behandling,
            kopierVedtakBegrunnelser = true,
        )

        val behandleUnderkjentVedtakTask =
            OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                tilordnetRessurs = totrinnskontroll.saksbehandlerId,
                fristForFerdigstillelse = LocalDate.now(),
            )
        taskRepository.save(behandleUnderkjentVedtakTask)

        return StegType.SEND_TIL_BESLUTTER
    }

    private fun publiserStønadsstatistikkVedFortsattInnvilgetEØS(behandling: Behandling) {
        if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET &&
            behandling.kategori == BehandlingKategori.EØS
        ) {
            val nyTaskV2 = PubliserVedtakV2Task.opprettTask(behandling.fagsak.aktør.aktivFødselsnummer(), behandling.id)
            taskRepository.save(nyTaskV2)
        }
    }

    private fun validerErTilbakekrevingHvisFeilutbetaling(
        behandlingId: Long,
    ) {
        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingId = behandlingId)
        val tilbakekrevingsvalg = tilbakekrevingService.hentTilbakekrevingsvalg(behandlingId = behandlingId)

        if (feilutbetaling != BigDecimal.ZERO && tilbakekrevingsvalg == null) {
            throw FunksjonellFeil("Det er en feilutbetaling som saksbehandler ikke har tatt stilling til. Saken må underkjennes og sendes tilbake til saksbehandler for ny vurdering.")
        }

        if (feilutbetaling == BigDecimal.ZERO && tilbakekrevingsvalg in listOf(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK, Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)) {
            throw FunksjonellFeil("Det er valgt å opprette tilbakekrevingssak men det er ikke lenger feilutbetalt beløp. Behandlingen må underkjennes, og saksbehandler må gå tilbake til behandlingsresultatet og trykke neste og fullføre behandlingen på nytt.")
        }
    }

    private fun sjekkOmBehandlingSkalIverksettesOgHentNesteSteg(behandling: Behandling): StegType {
        val endringerIUtbetaling = beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling)

        return hentNesteStegGittEndringerIUtbetaling(behandling, endringerIUtbetaling)
    }

    private fun opprettFerdigstillBehandlingTask(behandling: Behandling) {
        val ferdigstillBehandlingTask =
            FerdigstillBehandlingTask.opprettTask(
                søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingsId = behandling.id,
            )
        taskRepository.save(ferdigstillBehandlingTask)
    }

    private fun opprettTaskFerdigstillGodkjenneVedtak(
        behandling: Behandling,
        beslutning: BeslutningPåVedtakDto,
        behandlingErAutomatiskBesluttet: Boolean,
    ) {
        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = beslutning.beslutning,
            begrunnelse = beslutning.begrunnelse,
            behandlingErAutomatiskBesluttet = behandlingErAutomatiskBesluttet,
        )

        if (!behandling.erManuellMigrering() || !behandlingErAutomatiskBesluttet) {
            val ferdigstillGodkjenneVedtakTask = FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)
            taskRepository.save(ferdigstillGodkjenneVedtakTask)
        }
    }

    private fun opprettTaskIverksettMotOppdrag(
        behandling: Behandling,
        vedtak: Vedtak,
    ) {
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    private fun opprettJournalførVedtaksbrevTask(
        behandling: Behandling,
        vedtak: Vedtak,
    ) {
        val task =
            JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                vedtakId = vedtak.id,
                personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingId = behandling.id,
            )
        taskRepository.save(task)
    }

    private fun opprettJournalførTilbakekrevingsvedtakMotregningBrevTask(
        behandling: Behandling,
    ) {
        val task =
            JournalførTilbakekrevingsvedtakMotregningBrevTask.opprettTask(
                behandlingId = behandling.id,
            )
        taskRepository.save(task)
    }

    private fun validerBrevmottakere(
        behandlingId: BehandlingId,
        totrinnskontrollErGodkjent: Boolean,
    ) {
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId.id).map { ManuellBrevmottaker(it) }
        if (totrinnskontrollErGodkjent && !BrevmottakerValidering.erBrevmottakereGyldige(brevmottakere)) {
            throw FunksjonellFeil(
                melding = "Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og vedtaksbrevet kan ikke sendes. Behandlingen må underkjennes, og saksbehandler må legge til manuell adresse på nytt.",
            )
        }
    }
}
