package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestBehandlingStegTilstand
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestFødselshendelsefiltreringResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKompetanse
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestSettPåVent
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestTotrinnskontroll
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestVedtak
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.beregning.EndringstidspunktService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UtvidetBehandlingService(
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vedtakRepository: VedtakRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val fødselshendelsefiltreringResultatRepository: FødselshendelsefiltreringResultatRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val settPåVentService: SettPåVentService,
    private val kompetanseRepository: KompetanseRepository,
    private val endringstidspunktService: EndringstidspunktService,
    private val valutakursRepository: ValutakursRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val featureToggleService: FeatureToggleService,
) {

    fun lagRestUtvidetBehandling(behandlingId: Long): RestUtvidetBehandling {
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        val søknadsgrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
        val personer = personopplysningGrunnlag?.søkerOgBarn

        val arbeidsfordeling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId = behandling.id)

        val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)

        val personResultater = vilkårsvurderingService.hentAktivForBehandling(behandling.id)?.personResultater

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id))

        val totrinnskontroll =
            totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        val endringstidspunkt = endringstidspunktService.finnEndringstidpunkForBehandling(behandlingId)

        val kanBehandleEøs = featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)

        val kompetanser: List<Kompetanse> =
            if (kanBehandleEøs) kompetanseRepository.findByBehandlingId(behandlingId) else emptyList()

        val valutakurser =
            if (kanBehandleEøs) valutakursRepository.findByBehandlingId(behandlingId) else emptyList()

        val utenlandskePeriodebeløp =
            if (kanBehandleEøs) utenlandskPeriodebeløpRepository.findByBehandlingId(behandlingId) else emptyList()

        return RestUtvidetBehandling(
            behandlingId = behandling.id,
            steg = behandling.steg,
            stegTilstand = behandling.behandlingStegTilstand.map { it.tilRestBehandlingStegTilstand() },
            status = behandling.status,
            resultat = behandling.resultat,
            skalBehandlesAutomatisk = behandling.skalBehandlesAutomatisk,
            type = behandling.type,
            kategori = behandling.kategori,
            underkategori = behandling.underkategori,
            årsak = behandling.opprettetÅrsak,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            endretAv = behandling.endretAv,
            arbeidsfordelingPåBehandling = arbeidsfordeling.tilRestArbeidsfordelingPåBehandling(),
            søknadsgrunnlag = søknadsgrunnlag?.hentSøknadDto(),
            personer = personer?.map { persongrunnlagService.mapTilRestPersonMedStatsborgerskapLand(it) }
                ?: emptyList(),
            personResultater = personResultater?.map { it.tilRestPersonResultat() } ?: emptyList(),
            fødselshendelsefiltreringResultater = fødselshendelsefiltreringResultatRepository.finnFødselshendelsefiltreringResultater(
                behandlingId = behandling.id
            ).map { it.tilRestFødselshendelsefiltreringResultat() },
            utbetalingsperioder = vedtaksperiodeService.hentUtbetalingsperioder(behandling),
            personerMedAndelerTilkjentYtelse = personopplysningGrunnlag?.tilRestPersonerMedAndeler(andelerTilkjentYtelse)
                ?: emptyList(),
            endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)
                .map { it.tilRestEndretUtbetalingAndel() },
            tilbakekreving = tilbakekreving?.tilRestTilbakekreving(),
            endringstidspunkt = utledEndringstidpunkt(endringstidspunkt, behandling),
            vedtak = vedtak?.tilRestVedtak(
                vedtaksperioderMedBegrunnelser = if (behandling.status != BehandlingStatus.AVSLUTTET) {
                    vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak = vedtak).sortedBy { it.fom }
                } else emptyList(),
                skalMinimeres = behandling.status != BehandlingStatus.UTREDES
            ),
            kompetanser = kompetanser.map { it.tilRestKompetanse() },
            totrinnskontroll = totrinnskontroll?.tilRestTotrinnskontroll(),
            aktivSettPåVent = settPåVentService.finnAktivSettPåVentPåBehandling(behandlingId = behandlingId)
                ?.tilRestSettPåVent(),
            migreringsdato = behandlingService.hentMigreringsdatoIBehandling(behandlingId = behandlingId),
            valutakurser = valutakurser.map { it.tilRestValutakurs() },
            utenlandskePeriodebeløp = utenlandskePeriodebeløp.map { it.tilRestUtenlandskPeriodebeløp() }
        )
    }

    private fun utledEndringstidpunkt(
        endringstidspunkt: LocalDate,
        behandling: Behandling
    ) = when {
        endringstidspunkt == TIDENES_MORGEN || endringstidspunkt == TIDENES_ENDE -> null
        behandling.overstyrtEndringstidspunkt != null -> behandling.overstyrtEndringstidspunkt
        else -> endringstidspunkt
    }
}
