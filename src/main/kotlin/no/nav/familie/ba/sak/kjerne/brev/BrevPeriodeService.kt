package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.hentPerioderMedEndringerFra
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.dødeBarnForrigePeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.ytelseErFraForrigePeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
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
    private val integrasjonClient: IntegrasjonClient
) {

    fun hentBrevperioderData(
        vedtaksperioderId: List<Long>,
        behandlingId: BehandlingId,
        skalLogge: Boolean = true
    ): List<BrevperiodeData> {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val endredeUtbetalingAndeler = endretUtbetalingAndelService
            .hentForBehandling(behandlingId = behandlingId.id)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId.id)

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId.id)

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId.id)?.hentUregistrerteBarn()
                ?: emptyList()

        val kompetanser =
            kompetanseService.hentKompetanser(behandlingId = behandlingId)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val sanityEØSBegrunnelser =
            if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) sanityService.hentSanityEØSBegrunnelser() else emptyList()

        val restBehandlingsgrunnlagForBrev = hentRestBehandlingsgrunnlagForBrev(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag
        )

        return vedtaksperioderId.map {
            hentBrevperiodeData(
                vedtaksperiodeId = it,
                restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                uregistrerteBarn = uregistrerteBarn,
                skalLogge = skalLogge,
                kompetanser = kompetanser.toList(),
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEØSBegrunnelser = sanityEØSBegrunnelser
            )
        }
    }

    private fun hentBrevperiodeData(
        vedtaksperiodeId: Long,
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        uregistrerteBarn: List<BarnMedOpplysninger>,
        kompetanser: List<Kompetanse>,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,

        skalLogge: Boolean = true
    ): BrevperiodeData {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val minimerteUregistrerteBarn = uregistrerteBarn.map { it.tilMinimertUregisrertBarn() }

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse
        )

        val ytelserForrigePeriode =
            andelerTilkjentYtelse.filter { ytelseErFraForrigePeriode(it, utvidetVedtaksperiodeMedBegrunnelse) }

        val dødeBarnForrigePeriode =
            dødeBarnForrigePeriode(ytelserForrigePeriode, personopplysningGrunnlag.barna.tilMinimertePersoner())

        val minimertVedtaksperiode =
            utvidetVedtaksperiodeMedBegrunnelse.tilMinimertVedtaksperiode(
                sanityBegrunnelser = sanityBegrunnelser,
                sanityEØSBegrunnelser = sanityEØSBegrunnelser
            )

        val landkoderISO2 = integrasjonClient.hentLandkoderISO2()

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
            minimerteKompetanserForPeriode = hentMinimerteKompetanserForPeriode(
                kompetanser = kompetanser,
                fom = vedtaksperiodeMedBegrunnelser.fom?.toYearMonth(),
                tom = vedtaksperiodeMedBegrunnelser.tom?.toYearMonth(),
                personopplysningGrunnlag = personopplysningGrunnlag,
                landkoderISO2 = landkoderISO2
            ),
            minimerteKompetanserSomStopperRettFørPeriode = hentKompetanserSomStopperRettFørPeriode(
                kompetanser = kompetanser,
                periodeFom = minimertVedtaksperiode.fom?.toYearMonth()
            ).map {
                it.tilMinimertKompetanse(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    landkoderISO2 = landkoderISO2
                )
            },
            dødeBarnForrigePeriode = dødeBarnForrigePeriode
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
    ): List<String> =
        vedtaksperiodeMedBegrunnelser.vedtak.behandling
            .let { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(it) }
            ?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
            ?.let { andelerTilkjentYtelse.hentPerioderMedEndringerFra(it) }
            ?.filter { barn -> barn.value.any { it.fom == vedtaksperiodeMedBegrunnelser.fom } }
            ?.filter { !it.value.filterValue { beløp -> beløp < 0 }.isEmpty }
            ?.mapNotNull { personidentService.hentAktør(it.key).aktivFødselsnummer() }
            ?: emptyList()

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long) = hentBrevperioderData(
        vedtaksperioderId = listOf(vedtaksperiodeId),
        behandlingId = BehandlingId(vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId).vedtak.behandling.id)
    ).single().hentBegrunnelserOgFritekster()

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
