package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.config.FeatureToggleService
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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val featureToggleService: FeatureToggleService,
    private val personidentService: PersonidentService
) {

    fun hentBrevperiodeData(vedtaksperiodeId: Long, skalLogge: Boolean = true): BrevperiodeData {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        val behandlingId = vedtaksperiodeMedBegrunnelser.vedtak.behandling.id

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")
        val endredeUtbetalingAndeler =
            endretUtbetalingAndelService.hentForBehandling(behandlingId = behandlingId)
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentUregistrerteBarn()
                ?.map { it.tilMinimertUregisrertBarn() } ?: emptyList()

        val restBehandlingsgrunnlagForBrev = hentRestBehandlingsgrunnlagForBrev(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag
        )

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId
            ),
        )

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        val minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelse.tilMinimertVedtaksperiode(sanityBegrunnelser)

        val brevperiodeData = BrevperiodeData(
            minimertVedtaksperiode = minimertVedtaksperiode,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = uregistrerteBarn,
            brevMålform = personopplysningGrunnlag.søker.målform,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse,
                utvidetVedtaksperiodeMedBegrunnelse.fom
            ),
            barnPersonIdentMedReduksjon = hentBarnsPersonIdentMedRedusertPeriode(
                vedtaksperiodeMedBegrunnelser,
                andelerTilkjentYtelse
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

        val begrunnelseDataForVedtaksperiode = hentBrevperiodeData(vedtaksperiodeId)
        return begrunnelseDataForVedtaksperiode.hentBegrunnelserOgFritekster()
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
