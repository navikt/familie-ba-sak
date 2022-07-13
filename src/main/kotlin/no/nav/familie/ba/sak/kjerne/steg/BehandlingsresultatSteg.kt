package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseHentOgPersiserService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerBarnasVilkår
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BehandlingsresultatSteg(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val vilkårService: VilkårService,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val tilkjentYtelseHentOgPersiserService: TilkjentYtelseHentOgPersiserService,
) : BehandlingSteg<String> {

    override fun preValiderSteg(behandling: Behandling, stegService: StegService?) {
        if (behandling.skalBehandlesAutomatisk) return

        if (behandling.type != BehandlingType.TEKNISK_ENDRING && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandlingId = behandling.id)
            val barna = persongrunnlagService.hentBarna(behandling)

            validerBarnasVilkår(barna, vilkårsvurdering)
        }

        val tilkjentYtelse =
            tilkjentYtelseHentOgPersiserService.hentTilkjentYtelseForBehandlingThrows(behandlingId = behandling.id)
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

        validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
            tilkjentYtelse = tilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag
        )

        val endretUtbetalingAndeler = endretUtbetalingAndelService.hentForBehandling(behandling.id)
        validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler)
        validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler)
        validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(endretUtbetalingAndelerMedÅrsakDeltBosted = endretUtbetalingAndeler.filter { it.årsak == Årsak.DELT_BOSTED })
    }

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {

        val behandlingMedOppdatertBehandlingsresultat =
            if (behandling.erMigrering() && behandling.skalBehandlesAutomatisk) {
                settBehandlingsresultat(behandling, Behandlingsresultat.INNVILGET)
            } else {
                val resultat = behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)
                behandlingService.oppdaterBehandlingsresultat(
                    behandlingId = behandling.id,
                    resultat = resultat
                )
            }

        if (behandlingMedOppdatertBehandlingsresultat.erBehandlingMedVedtaksbrevutsending()) {
            behandlingService.nullstillEndringstidspunkt(behandling.id)
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(
                    behandlingId = behandling.id
                )
            )
        }

        if (behandlingMedOppdatertBehandlingsresultat.skalRettFraBehandlingsresultatTilIverksetting() ||
            beregningService.kanAutomatiskIverksetteSmåbarnstilleggEndring(
                    behandling = behandlingMedOppdatertBehandlingsresultat,
                    sistIverksatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(
                            behandling = behandlingMedOppdatertBehandlingsresultat
                        )
                )
        ) {
            behandlingService.oppdaterStatusPåBehandling(
                behandlingMedOppdatertBehandlingsresultat.id,
                BehandlingStatus.IVERKSETTER_VEDTAK
            )
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedOppdatertBehandlingsresultat)
        }

        return hentNesteStegForNormalFlyt(behandlingMedOppdatertBehandlingsresultat)
    }

    override fun stegType(): StegType {
        return StegType.BEHANDLINGSRESULTAT
    }

    private fun settBehandlingsresultat(behandling: Behandling, resultat: Behandlingsresultat): Behandling {
        behandling.resultat = resultat
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }
}
