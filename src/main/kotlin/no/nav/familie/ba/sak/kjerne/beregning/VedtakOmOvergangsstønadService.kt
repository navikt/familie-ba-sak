package no.nav.familie.ba.sak.kjerne.beregning

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class VedtakOmOvergangsstønadService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val stegService: StegService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val beregningService: BeregningService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val taskRepository: TaskRepository
) {

    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall")
    private val antallVedtakOmOvergangsstønadÅpenBehandling: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "aapen_behandling")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak")

    fun håndterVedtakOmOvergangsstønad(personIdent: String) {
        antallVedtakOmOvergangsstønad.increment()

        val fagsak = fagsakService.hent(personIdent = PersonIdent(personIdent)) ?: return
        val harÅpenBehandling = behandlingService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) != null

        if (harÅpenBehandling) {
            antallVedtakOmOvergangsstønadÅpenBehandling.increment()
            // TODO lag oppgave
            return
        }

        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)

        if (påvirkerFagsak) {
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

            leggTilBegrunnelserPåVedtak(fagsak, behandlingEtterBehandlingsresultat)

            val vedtakEtterTotrinn = vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

            val task = IverksettMotOppdragTask.opprettTask(
                behandlingEtterBehandlingsresultat,
                vedtakEtterTotrinn,
                SikkerhetContext.hentSaksbehandler()
            )
            taskRepository.save(task)
        } else {
            antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak.increment()
        }
    }

    private fun leggTilBegrunnelserPåVedtak(
        fagsak: Fagsak,
        behandlingEtterBehandlingsresultat: Behandling
    ) {
        val sistIverksatteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id)
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = sistIverksatteBehandling.id
            ).filter { it.erSmåbarnstillegg() }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) emptyList()
            else beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                behandlingId = behandlingEtterBehandlingsresultat.id
            ).filter { it.erSmåbarnstillegg() }

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterBehandlingsresultat.id)
        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        populerBegrunnelser(
            vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            vedtak = vedtak,
            søkersIdent = fagsak.hentAktivIdent().ident,
            månedPerioder = hentReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
                nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler
            ),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        populerBegrunnelser(
            vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
            vedtak = vedtak,
            søkersIdent = fagsak.hentAktivIdent().ident,
            månedPerioder = nyeSmåbarnstilleggAndeler.map { MånedPeriode(fom = it.stønadFom, tom = it.stønadTom) },
            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING
        )
    }

    private fun populerBegrunnelser(
        vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak,
        søkersIdent: String,
        månedPerioder: List<MånedPeriode>,
        vedtaksperiodetype: Vedtaksperiodetype
    ) {
        månedPerioder.forEach { månedPeriode ->
            val vedtaksperiodeMedBegrunnelser =
                vedtaksperioderMedBegrunnelser.find {
                    it.fom == månedPeriode.fom.førsteDagIInneværendeMåned() &&
                        it.tom == månedPeriode.tom.sisteDagIInneværendeMåned() &&
                        it.type == vedtaksperiodetype
                }

            if (vedtaksperiodeMedBegrunnelser != null) {
                vedtaksperiodeMedBegrunnelser.settBegrunnelser(
                    (vedtaksperiodeMedBegrunnelser.begrunnelser +
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER,
                            personIdenter = listOf(søkersIdent)
                        )).toList()
                )

                vedtaksperiodeService.lagre(vedtaksperiodeMedBegrunnelser)
            } else {
                vedtaksperiodeService.lagre(
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = vedtak,
                        fom = månedPeriode.fom.førsteDagIInneværendeMåned(),
                        tom = månedPeriode.tom.sisteDagIInneværendeMåned(),
                        type = vedtaksperiodetype
                    )
                        .apply {
                            begrunnelser.add(
                                Vedtaksbegrunnelse(
                                    vedtaksperiodeMedBegrunnelser = this,
                                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER,
                                    personIdenter = listOf(søkersIdent)
                                )
                            )
                        }
                )
            }
        }
    }
}
