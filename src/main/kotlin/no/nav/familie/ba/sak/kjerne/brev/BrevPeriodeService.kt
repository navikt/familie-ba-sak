package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.Beløpsdifferanse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.hentPerioderMedEndringerFra
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val personidentService: PersonidentService,
    private val kompetanseService: KompetanseService,
    private val featureToggleService: FeatureToggleService,
) {

    fun hentBrevperioderData(
        vedtaksperioderId: List<Long>,
        behandlingId: BehandlingId,
        skalLogge: Boolean = true,
    ): List<BrevperiodeData> {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val endredeUtbetalingAndeler =
            endretUtbetalingAndelService.hentForBehandling(behandlingId = behandlingId.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id)

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
            behandlingId.id
        )

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId.id)?.hentUregistrerteBarn()
                ?: emptyList()

        val kompetanser =
            kompetanseService.hentKompetanser(behandlingId = behandlingId)

        return vedtaksperioderId.map {
            hentBrevperiodeData(
                vedtaksperiodeId = it,
                vilkårsvurdering = vilkårsvurdering,
                endredeUtbetalingAndeler = endredeUtbetalingAndeler,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                uregistrerteBarn = uregistrerteBarn,
                skalLogge = skalLogge,
                kompetanser = kompetanser.toList(),
            )
        }
    }

    private fun hentBrevperiodeData(
        vedtaksperiodeId: Long,
        vilkårsvurdering: Vilkårsvurdering,
        endredeUtbetalingAndeler: List<EndretUtbetalingAndel>,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        uregistrerteBarn: List<BarnMedOpplysninger>,
        kompetanser: List<Kompetanse>,

        skalLogge: Boolean = true,
    ): BrevperiodeData {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val sanityEØSBegrunnelser =
            if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) sanityService.hentSanityEØSBegrunnelser() else emptyList()

        val restBehandlingsgrunnlagForBrev = hentRestBehandlingsgrunnlagForBrev(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag
        )

        val minimerteUregistrerteBarn = uregistrerteBarn.map { it.tilMinimertUregisrertBarn() }

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )

        val minimertVedtaksperiode =
            utvidetVedtaksperiodeMedBegrunnelse.tilMinimertVedtaksperiode(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEØSBegrunnelser = sanityEØSBegrunnelser
            )

        val brevperiodeData = BrevperiodeData(
            minimertVedtaksperiode = minimertVedtaksperiode,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = minimerteUregistrerteBarn,
            brevMålform = personopplysningGrunnlag.søker.målform,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                periodeFom = utvidetVedtaksperiodeMedBegrunnelse.fom
            ),
            barnMedReduksjonFraForrigeBehandlingIdent = hentBarnsPersonIdentMedRedusertPeriode(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                andelerTilkjentYtelse = andelerTilkjentYtelse
            ),
            minimerteKompetanser = hentMinimerteKompetanserForPeriode(
                kompetanser = kompetanser,
                fom = vedtaksperiodeMedBegrunnelser.fom?.toYearMonth(),
                tom = vedtaksperiodeMedBegrunnelser.tom?.toYearMonth(),
                personopplysningGrunnlag = personopplysningGrunnlag
            )

        )

        if (skalLogge) {
            secureLogger.info(
                "Data for brevperiode på behandling ${vedtaksperiodeMedBegrunnelser.vedtak.behandling}: \n" +
                    brevperiodeData.tilBrevperiodeForLogging().convertDataClassToJson()
            )
        }

        return brevperiodeData
    }

    private fun hentBarnsPersonIdentMedRedusertPeriode(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ): List<String> {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(vedtaksperiodeMedBegrunnelser.vedtak.behandling)
        return if (forrigeBehandling != null) {
            val forrigeAndelTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id)
            val endringerITilkjentYtelsePerBarn =
                andelerTilkjentYtelse.hentPerioderMedEndringerFra(forrigeAndelTilkjentYtelse)
            endringerITilkjentYtelsePerBarn.keys.filter { barn ->
                endringerITilkjentYtelsePerBarn.getValue(barn).any {
                    it.overlapper(
                        LocalDateSegment(
                            vedtaksperiodeMedBegrunnelser.fom,
                            vedtaksperiodeMedBegrunnelser.tom,
                            null
                        )
                    )
                }
            }.mapNotNull { barn ->
                val result: LocalDateTimeline<Beløpsdifferanse> = endringerITilkjentYtelsePerBarn.getValue(barn)
                if (!result.filterValue { beløp -> beløp < 0 }.isEmpty) {
                    personidentService.hentAktør(barn).aktivFødselsnummer()
                } else null
            }
        } else emptyList()
    }

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {

        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val begrunnelseDataForVedtaksperiode =
            hentBrevperioderData(
                vedtaksperioderId = listOf(vedtaksperiodeId),
                behandlingId = BehandlingId(vedtaksperiodeMedBegrunnelser.vedtak.behandling.id)
            ).single()
        return begrunnelseDataForVedtaksperiode.hentBegrunnelserOgFritekster()
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
