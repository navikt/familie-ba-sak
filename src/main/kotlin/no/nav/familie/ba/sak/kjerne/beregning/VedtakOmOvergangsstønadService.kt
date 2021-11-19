package no.nav.familie.ba.sak.kjerne.beregning

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.erÅpen
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class VedtakOmOvergangsstønadService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val stegService: StegService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val simuleringService: SimuleringService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val taskRepository: TaskRepository,
    private val featureToggleService: FeatureToggleService
) {

    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall")
    private val antallVedtakOmOvergangsstønadÅpenBehandling: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "aapen_behandling")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak")
    private val antallVedtakOmOvergangsstønadFeilutbetaling: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "feilutbetaling")
    private val antallVedtakOmOvergangsstønadEtterbetaling: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "etterbetaling")

    fun håndterVedtakOmOvergangsstønad(personIdent: String): String {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_SMÅBARNSTILLEGG_AUTOMATISK))
            return "automatisk behandling av småbarnstillegg er ikke påskrudd"

        antallVedtakOmOvergangsstønad.increment()

        val fagsak = fagsakService.hent(personIdent = PersonIdent(personIdent)) ?: return "har ikke fagsak i systemet"
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)

        if (aktivBehandling == null) {
            return "har ikke vært noe behandling på fagsak, ingenting å revurdere"
        } else if (aktivBehandling.status.erÅpen()) {
            antallVedtakOmOvergangsstønadÅpenBehandling.increment()
            // TODO lag oppgave
            return "har åpen behandling, lager oppgave"
        }

        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)

        return if (påvirkerFagsak) {
            antallVedtakOmOvergangsstønadPåvirkerFagsak.increment()

            val nyBehandling = stegService.håndterNyBehandling(
                NyBehandling(
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                    søkersIdent = fagsak.hentAktivIdent().ident,
                    skalBehandlesAutomatisk = true
                )
            )

            val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(nyBehandling)
            val behandlingEtterBehandlingsresultat =
                stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)

            if (behandlingEtterBehandlingsresultat.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
                val feilutbetaling =
                    simuleringService.hentFeilutbetaling(behandlingId = behandlingEtterBehandlingsresultat.id)
                val etterbetaling =
                    simuleringService.hentEtterbetaling(behandlingId = behandlingEtterBehandlingsresultat.id)

                if (feilutbetaling != BigDecimal(0)) {
                    antallVedtakOmOvergangsstønadFeilutbetaling.increment()
                    behandlingService.omgjørTilManuellBehandling(behandlingEtterBehandlingsresultat)
                    // TODO opprett oppgave
                    return "behandling fører til feilutbetaling, må fortsette behandling manuelt."
                } else if (etterbetaling != BigDecimal(0)) {
                    antallVedtakOmOvergangsstønadEtterbetaling.increment()
                    behandlingService.omgjørTilManuellBehandling(behandlingEtterBehandlingsresultat)
                    // TODO opprett oppgave
                    return "behandling fører til etterbetaling, må fortsette behandling manuelt."
                }
            }

            leggTilBegrunnelserPåVedtak(behandlingEtterBehandlingsresultat)

            val vedtakEtterTotrinn = vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

            val task = IverksettMotOppdragTask.opprettTask(
                behandlingEtterBehandlingsresultat,
                vedtakEtterTotrinn,
                SikkerhetContext.hentSaksbehandler()
            )
            taskRepository.save(task)

            "påvirker fagsak, autovedtak kjørt vellykket"
        } else {
            antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak.increment()

            "påvirker ikke fagsak"
        }
    }

    private fun leggTilBegrunnelserPåVedtak(behandlingEtterBehandlingsresultat: Behandling) {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterBehandlingsresultat.id)

        val vedtaksperiodeSomSkalOppdateres =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                utvidedeVedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(
                    vedtak
                ),
                vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
            )

        vedtaksperiodeService.lagre(vedtaksperiodeSomSkalOppdateres)
    }
}
