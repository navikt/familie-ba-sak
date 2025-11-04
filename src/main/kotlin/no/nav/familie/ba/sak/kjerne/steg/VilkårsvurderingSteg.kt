package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.finnesPerioderDerBarnMedDeltBostedIkkeBorMedSøkerIFinnmark
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.finnesPerioderDerBarnMedDeltBostedIkkeBorMedSøkerPåSvalbard
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.valider18ÅrsVilkårEksistererFraFødselsdato
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerAtManIkkeBorIBådeFinnmarkOgSvalbardSamtidig
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerAtVilkårsvurderingErEndret
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerBarnasVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIkkeBlandetRegelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.validerIngenVilkårSattEtterSøkersDød
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class VilkårsvurderingSteg(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingstemaService: BehandlingstemaService,
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val tilpassKompetanserTilRegelverkService: TilpassKompetanserTilRegelverkService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val månedligValutajusteringService: MånedligValutajusteringService,
    private val clockProvider: ClockProvider,
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val featureToggleService: FeatureToggleService,
    private val opprettTaskService: OpprettTaskService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : BehandlingSteg<List<String>?> {
    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
            validerIngenVilkårSattEtterSøkersDød(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = vilkårsvurdering,
            )
        }

        vilkårService.hentVilkårsvurdering(behandling.id)?.apply {
            validerIkkeBlandetRegelverk(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = this,
                behandling = behandling,
            )

            valider18ÅrsVilkårEksistererFraFødselsdato(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = this,
                behandling = behandling,
            )

            validerAtManIkkeBorIBådeFinnmarkOgSvalbardSamtidig(
                søkerOgBarn = søkerOgBarn,
                vilkårsvurdering = this,
            )

            if (behandling.erFinnmarksEllerSvalbardtillegg()) {
                validerFinnmarkOgSvalbardBehandling(behandling = behandling, vilkårsvurdering = this)
            }
        }
    }

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: List<String>?,
    ): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        if (behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = true,
                forrigeBehandlingSomErVedtatt =
                    behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                        behandling,
                    ),
                barnSomSkalVurderesIFødselshendelse = data,
            )
        }

        tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling)

        beregningService.genererTilkjentYtelseFraVilkårsvurdering(behandling, personopplysningGrunnlag)

        if (featureToggleService.isEnabled(FeatureToggle.PREUTFYLLING_ENDRET_UTBETALING_3ÅR_ELLER_3MND)) {
            endretUtbetalingAndelService.genererEndretUtbetalingAndelerMedÅrsakEtterbetaling3ÅrEller3Mnd(behandling)
        }

        if (!behandling.erSatsendring()) {
            tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id))
        }

        if (
            behandling.type in listOf(BehandlingType.REVURDERING, BehandlingType.TEKNISK_ENDRING) &&
            !behandling.skalBehandlesAutomatisk
        ) {
            automatiskOppdaterValutakursService.resettValutakurserOgLagValutakurserEtterEndringstidspunkt(BehandlingId(behandling.id))
        }

        if (behandling.erMånedligValutajustering()) {
            månedligValutajusteringService.oppdaterValutakurserForMåned(BehandlingId(behandling.id), YearMonth.now(clockProvider.get()))
        }

        automatiskOppdaterValutakursService.oppdaterAndelerMedValutakurser(BehandlingId(behandling.id))

        behandlingstemaService.oppdaterBehandlingstemaForVilkår(
            behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id),
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (behandling.type != BehandlingType.TEKNISK_ENDRING && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandlingId = behandling.id)
            val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id)

            validerBarnasVilkår(søkerOgBarn.barn(), vilkårsvurdering)
        }
    }

    private fun validerFinnmarkOgSvalbardBehandling(
        behandling: Behandling,
        vilkårsvurdering: Vilkårsvurdering,
    ) {
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        if (forrigeVedtatteBehandling == null) {
            throw Feil("Kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}")
        }

        val andelerIForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeVedtatteBehandling.id)

        if (behandling.erFinnmarkstillegg()) {
            val skalBehandlesManuelt = finnesPerioderDerBarnMedDeltBostedIkkeBorMedSøkerIFinnmark(vilkårsvurdering, andelerIForrigeBehandling)
            if (skalBehandlesManuelt && featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD)) {
                opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                    fagsakId = behandling.fagsak.id,
                    beskrivelse = "Finnmarkstillegg kan ikke behandles automatisk som følge av adresseendring.\nDet finnes perioder der søker er bosatt i Finnmark/Nord-Troms samtidig som et barn med delt barnetrygd ikke er bosatt i Finnmark/Nord-Troms.",
                )
            }
        }

        if (behandling.erSvalbardtillegg()) {
            val skalBehandlesManuelt = finnesPerioderDerBarnMedDeltBostedIkkeBorMedSøkerPåSvalbard(vilkårsvurdering, andelerIForrigeBehandling)
            if (skalBehandlesManuelt && featureToggleService.isEnabled(FeatureToggle.OPPRETT_MANUELL_OPPGAVE_AUTOVEDTAK_FINNMARK_SVALBARD)) {
                opprettTaskService.opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
                    fagsakId = behandling.fagsak.id,
                    beskrivelse = "Svalbardtillegg kan ikke behandles automatisk som følge av adresseendring.\nDet finnes perioder der søker er bosatt på Svalbard samtidig som et barn med delt barnetrygd ikke er bosatt på Svalbard.",
                )
            }
        }

        validerAtVilkårsvurderingErEndret(
            vilkårsvurdering = vilkårsvurdering,
            forrigeVilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(forrigeVedtatteBehandling.id),
        )
    }

    override fun stegType(): StegType = StegType.VILKÅRSVURDERING
}
