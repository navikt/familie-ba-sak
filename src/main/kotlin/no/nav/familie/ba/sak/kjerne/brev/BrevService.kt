package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstavIAlleNavn
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.Companion.AUTOMATISK_BEHANDLING_BREVSIGNATUR
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.finnInnvilgedeOgReduserteFinnmarkstilleggPerioder
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.finnInnvilgedeOgReduserteSvalbardtilleggPerioder
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.filtrerBortIrrelevanteAndeler
import no.nav.familie.ba.sak.kjerne.beregning.AvregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.BrevBegrunnelseFeil
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.lagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakEndring
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakFinnmarkstillegg
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakNyfødtBarnFraFør
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakNyfødtFørsteBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakSvalbardtillegg
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Avslag
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Dødsfall
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.DødsfallData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EtterbetalingInstitusjon
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForsattInnvilget
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorreksjonVedtaksbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorreksjonVedtaksbrevData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.OpphørMedEndring
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.OpphørMedEndringSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Opphørt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.TilbakekrevingsvedtakMotregningBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.TilbakekrevingsvedtakMotregningBrevData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.UtbetalingstabellAutomatiskValutajustering
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakEndringSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakFellesfelterSammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs
import no.nav.familie.ba.sak.kjerne.brev.hjemler.HjemmeltekstUtleder
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingService
import no.nav.familie.ba.sak.kjerne.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class BrevService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val simuleringService: SimuleringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val organisasjonService: OrganisasjonService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val brevmalService: BrevmalService,
    private val kodeverkService: KodeverkService,
    private val testVerktøyService: TestVerktøyService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val kompetanseRepository: KompetanseRepository,
    private val valutakursRepository: ValutakursRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val hjemmeltekstUtleder: HjemmeltekstUtleder,
    private val avregningService: AvregningService,
    private val featureToggleService: FeatureToggleService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun hentVedtaksbrevData(vedtak: Vedtak): Vedtaksbrev {
        val behandling = vedtak.behandling

        val brevmal =
            brevmalService.hentBrevmal(
                behandling,
            )

        val vedtakFellesfelter = lagVedtaksbrevFellesfelter(vedtak)
        validerBrevdata(brevmal, vedtakFellesfelter)

        val skalMeldeFraOmEndringerEøsSelvstendigRett by lazy {
            vedtaksperiodeService.skalMeldeFraOmEndringerEøsSelvstendigRett(vedtak)
        }

        val skalInkludereInformasjonOmUtbetaling by lazy {
            sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)
        }

        return when (brevmal) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> {
                Førstegangsvedtak(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMåMeldeFraOmEndringer = !skalMeldeFraOmEndringerEøsSelvstendigRett,
                    duMåMeldeFraOmEndringerEøsSelvstendigRett = skalMeldeFraOmEndringerEøsSelvstendigRett,
                    informasjonOmUtbetaling = skalInkludereInformasjonOmUtbetaling,
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelter.utbetalingerPerMndEøs),
                )
            }

            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON -> {
                Førstegangsvedtak(
                    mal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                )
            }

            Brevmal.VEDTAK_ENDRING -> {
                VedtakEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erKlage = behandling.erKlage(),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    feilutbetaltValuta =
                        vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)?.let {
                            FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                        },
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMåMeldeFraOmEndringer = !skalMeldeFraOmEndringerEøsSelvstendigRett,
                    duMåMeldeFraOmEndringerEøsSelvstendigRett = skalMeldeFraOmEndringerEøsSelvstendigRett,
                    informasjonOmUtbetaling = skalInkludereInformasjonOmUtbetaling,
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelter.utbetalingerPerMndEøs),
                )
            }

            Brevmal.VEDTAK_ENDRING_INSTITUSJON -> {
                VedtakEndring(
                    mal = Brevmal.VEDTAK_ENDRING_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                    erKlage = behandling.erKlage(),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                )
            }

            Brevmal.VEDTAK_OPPHØRT -> {
                Opphørt(
                    vedtakFellesfelter = vedtakFellesfelter,
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                )
            }

            Brevmal.VEDTAK_OPPHØRT_INSTITUSJON -> {
                Opphørt(
                    mal = Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                )
            }

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING -> {
                OpphørMedEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    erKlage = behandling.erKlage(),
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelter.utbetalingerPerMndEøs),
                )
            }

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON -> {
                OpphørMedEndring(
                    mal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    erKlage = behandling.erKlage(),
                )
            }

            Brevmal.VEDTAK_AVSLAG -> {
                Avslag(vedtakFellesfelter = vedtakFellesfelter)
            }

            Brevmal.VEDTAK_AVSLAG_INSTITUSJON -> {
                Avslag(
                    mal = Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                )
            }

            Brevmal.VEDTAK_FORTSATT_INNVILGET -> {
                ForsattInnvilget(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMåMeldeFraOmEndringer = !skalMeldeFraOmEndringerEøsSelvstendigRett,
                    duMåMeldeFraOmEndringerEøsSelvstendigRett = skalMeldeFraOmEndringerEøsSelvstendigRett,
                    informasjonOmUtbetaling = skalInkludereInformasjonOmUtbetaling,
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelter.utbetalingerPerMndEøs),
                )
            }

            Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON -> {
                ForsattInnvilget(
                    mal = Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                )
            }

            Brevmal.AUTOVEDTAK_ENDRING -> {
                AutovedtakEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    innvilgetSvalbardtillegg = sjekkOmDetErNyInnvilgetSvalbardtilleggIBehandling(behandling),
                    innvilgetFinnmarkstillegg = sjekkOmDetErNyInnvilgetFinnmarkstilleggIBehandling(behandling),
                )
            }

            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN -> {
                AutovedtakNyfødtFørsteBarn(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                )
            }

            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> {
                AutovedtakNyfødtBarnFraFør(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                )
            }

            Brevmal.AUTOVEDTAK_FINNMARKSTILLEGG -> {
                AutovedtakFinnmarkstillegg(
                    vedtakFellesfelter = vedtakFellesfelter,
                )
            }

            Brevmal.AUTOVEDTAK_SVALBARDTILLEGG -> {
                AutovedtakSvalbardtillegg(
                    vedtakFellesfelter = vedtakFellesfelter,
                )
            }

            else -> {
                throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${brevmal.visningsTekst}")
            }
        }
    }

    private fun hentLandOgStartdatoForUtbetalingstabell(
        vedtak: Vedtak,
        utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>?,
    ): UtbetalingstabellAutomatiskValutajustering? {
        val behandlingId = vedtak.behandling.id

        return utbetalingerPerMndEøs?.let {
            val endringstidspunkt = finnStarttidspunktForUtbetalingstabell(behandling = vedtak.behandling)
            val landkoder = kodeverkService.hentLandkoderISO2()
            val kompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId = behandlingId)
            return hentLandOgStartdatoForUtbetalingstabell(endringstidspunkt.toYearMonth(), landkoder, kompetanser)
        }
    }

    fun sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling: Behandling): Boolean {
        val andelerIBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val andelerIBehandlingSomErDifferanseBeregnet = andelerIBehandling.filter { it.differanseberegnetPeriodebeløp != null }
        val andelerIBehandlingSomErDifferanseBeregnetOgLøpende = andelerIBehandlingSomErDifferanseBeregnet.filter { it.erLøpende() }

        return andelerIBehandlingSomErDifferanseBeregnetOgLøpende.isNotEmpty()
    }

    fun sjekkOmDetErNyInnvilgetFinnmarkstilleggIBehandling(behandling: Behandling): Boolean =
        sjekkOmDetErNyInnvilgetTilleggIBehandling(
            behandling = behandling,
            finnPerioderMedNyInnvilgetAndeler = ::finnInnvilgedeOgReduserteFinnmarkstilleggPerioder,
        )

    fun sjekkOmDetErNyInnvilgetSvalbardtilleggIBehandling(behandling: Behandling): Boolean =
        sjekkOmDetErNyInnvilgetTilleggIBehandling(
            behandling = behandling,
            finnPerioderMedNyInnvilgetAndeler = ::finnInnvilgedeOgReduserteSvalbardtilleggPerioder,
        )

    private fun sjekkOmDetErNyInnvilgetTilleggIBehandling(
        behandling: Behandling,
        finnPerioderMedNyInnvilgetAndeler: (forrige: List<AndelTilkjentYtelse>, nåværende: List<AndelTilkjentYtelse>) -> Pair<Set<YearMonth>, Set<YearMonth>>,
    ): Boolean {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService
                .hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
                ?: throw Feil("Finner ikke siste iverksatte behandling")

        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sistIverksatteBehandling.id)
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val (innvilgedePerioder, _) = finnPerioderMedNyInnvilgetAndeler(forrigeAndeler, nåværendeAndeler)

        return innvilgedePerioder.isNotEmpty()
    }

    private fun beskrivPerioderMedUavklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService
            .beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = false)
            ?.let { RefusjonEøsUavklart(perioderMedRefusjonEøsUavklart = it) }

    private fun beskrivPerioderMedAvklartRefusjonEøs(vedtak: Vedtak) =
        vedtaksperiodeService
            .beskrivPerioderMedRefusjonEøs(behandling = vedtak.behandling, avklart = true)
            ?.let { RefusjonEøsAvklart(perioderMedRefusjonEøsAvklart = it) }

    private fun validerBrevdata(
        brevmal: Brevmal,
        vedtakFellesfelter: VedtakFellesfelter,
    ) {
        if (brevmal in
            listOf(
                Brevmal.VEDTAK_OPPHØRT,
                Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
            ) &&
            vedtakFellesfelter.perioder.size > 1
        ) {
            throw FunksjonellFeil(
                "Behandlingsstatusen er \"Opphørt\", men mer enn én periode er begrunnet. Du skal kun begrunne perioden uten utbetaling.",
            )
        }
    }

    fun hentDødsfallbrevData(vedtak: Vedtak): Brev =
        hentGrunnlagOgSignaturData(vedtak.behandling).let { data ->
            Dødsfall(
                data =
                    DødsfallData(
                        delmalData =
                            DødsfallData.DelmalData(
                                signaturVedtak =
                                    SignaturVedtak(
                                        enhet = data.enhet,
                                        saksbehandler = data.saksbehandler,
                                        beslutter = data.beslutter,
                                    ),
                            ),
                        flettefelter =
                            DødsfallData.Flettefelter(
                                navn = data.grunnlag.søker.navn,
                                fodselsnummer =
                                    data.grunnlag.søker.aktør
                                        .aktivFødselsnummer(),
                                // Selv om det er feil å anta at alle navn er på dette formatet er det ønskelig å skrive
                                // det slik, da uppercase kan oppleves som skrikende i et brev som skal være skånsomt
                                navnAvdode =
                                    data.grunnlag.søker.navn
                                        .storForbokstavIAlleNavn(),
                                virkningstidspunkt =
                                    hentVirkningstidspunktForDødsfallbrev(
                                        opphørsperioder = vedtaksperiodeService.finnVedtaksperioderForBehandling(vedtak).filter { it.type == Vedtaksperiodetype.OPPHØR },
                                        behandlingId = vedtak.behandling.id,
                                    ),
                            ),
                    ),
            )
        }

    fun hentKorreksjonbrevData(vedtak: Vedtak): Brev =
        hentGrunnlagOgSignaturData(vedtak.behandling).let { data ->
            KorreksjonVedtaksbrev(
                data =
                    KorreksjonVedtaksbrevData(
                        delmalData =
                            KorreksjonVedtaksbrevData.DelmalData(
                                signaturVedtak =
                                    SignaturVedtak(
                                        enhet = data.enhet,
                                        saksbehandler = data.saksbehandler,
                                        beslutter = data.beslutter,
                                    ),
                            ),
                        flettefelter =
                            KorreksjonVedtaksbrevData.Flettefelter(
                                navn = data.grunnlag.søker.navn,
                                fodselsnummer =
                                    data.grunnlag.søker.aktør
                                        .aktivFødselsnummer(),
                            ),
                    ),
            )
        }

    fun hentSammensattKontrollsakBrevdata(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): Vedtaksbrev {
        val behandling = vedtak.behandling

        val brevmal =
            brevmalService.hentBrevmal(
                vedtak.behandling,
            )

        val skalMeldeFraOmEndringerEøsSelvstendigRett by lazy {
            vedtaksperiodeService.skalMeldeFraOmEndringerEøsSelvstendigRett(vedtak)
        }

        val skalInkludereInformasjonOmUtbetaling by lazy {
            sjekkOmDetErLøpendeDifferanseUtbetalingPåBehandling(behandling)
        }

        val vedtakFellesfelterSammensattKontrollsak = lagVedtaksbrevFellesfelterSammensattKontrollsak(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak)
        return when (brevmal) {
            Brevmal.VEDTAK_OPPHØRT -> {
                OpphørtSammensattKontrollsak(
                    vedtakFellesfelterSammensattKontrollsak = vedtakFellesfelterSammensattKontrollsak,
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                )
            }

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING -> {
                OpphørMedEndringSammensattKontrollsak(
                    vedtakFellesfelter = vedtakFellesfelterSammensattKontrollsak,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    erKlage = behandling.erKlage(),
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelterSammensattKontrollsak.utbetalingerPerMndEøs),
                )
            }

            Brevmal.VEDTAK_ENDRING -> {
                VedtakEndringSammensattKontrollsak(
                    vedtakFellesfelter = vedtakFellesfelterSammensattKontrollsak,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erKlage = behandling.erKlage(),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                    informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                    feilutbetaltValuta =
                        vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)?.let {
                            FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                        },
                    refusjonEosAvklart = beskrivPerioderMedAvklartRefusjonEøs(vedtak),
                    refusjonEosUavklart = beskrivPerioderMedUavklartRefusjonEøs(vedtak),
                    duMåMeldeFraOmEndringer = !skalMeldeFraOmEndringerEøsSelvstendigRett,
                    duMåMeldeFraOmEndringerEøsSelvstendigRett = skalMeldeFraOmEndringerEøsSelvstendigRett,
                    informasjonOmUtbetaling = skalInkludereInformasjonOmUtbetaling,
                    utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(vedtak, vedtakFellesfelterSammensattKontrollsak.utbetalingerPerMndEøs),
                )
            }

            else -> {
                throw Feil("Brevmalen $brevmal er ikke støttet for sammensatte kontrollsaker")
            }
        }
    }

    private fun hentSorterteVedtaksperioderMedBegrunnelser(vedtak: Vedtak) =
        vedtaksperiodeService
            .hentPersisterteVedtaksperioder(vedtak)
            .filter { it.erBegrunnet() }
            .sortedBy { it.fom }

    fun lagVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        val sorterteVedtaksperioderMedBegrunnelser = hentSorterteVedtaksperioderMedBegrunnelser(vedtak)

        if (sorterteVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet.",
            )
        }

        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak.behandling)

        val behandlingId = vedtak.behandling.id

        val grunnlagForBegrunnelser = vedtaksperiodeService.hentGrunnlagForBegrunnelse(vedtak.behandling)
        val brevperioder =
            sorterteVedtaksperioderMedBegrunnelser.mapNotNull { vedtaksperiode ->
                try {
                    vedtaksperiode.lagBrevPeriode(
                        grunnlagForBegrunnelse = grunnlagForBegrunnelser,
                        landkoder = kodeverkService.hentLandkoderISO2(),
                    )
                } catch (e: BrevBegrunnelseFeil) {
                    secureLogger.warn(
                        "Brevbegrunnelsefeil for behandling $behandlingId, " +
                            "fagsak ${vedtak.behandling.fagsak.id} " +
                            "på periode ${vedtaksperiode.fom} - ${vedtaksperiode.tom}. " +
                            "\nAutogenerert test:\n" + testVerktøyService.hentBegrunnelsetest(behandlingId),
                    )
                    throw e
                }
            }

        val utbetalingerPerMndEøs = hentUtbetalingerPerMndEøs(vedtak)

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId)

        val hjemmeltekst =
            hjemmeltekstUtleder.utledHjemmeltekst(
                behandlingId = behandlingId,
                vedtakKorrigertHjemmelSkalMedIBrev = korrigertVedtak != null,
                sorterteVedtaksperioderMedBegrunnelser = sorterteVedtaksperioderMedBegrunnelser,
            )

        val organisasjonsnummer =
            vedtak.behandling.fagsak.institusjon
                ?.orgNummer
        val organisasjonsnavn = organisasjonsnummer?.let { organisasjonService.hentOrganisasjon(it).navn }

        return VedtakFellesfelter(
            enhet = grunnlagOgSignaturData.enhet,
            saksbehandler = grunnlagOgSignaturData.saksbehandler,
            beslutter = grunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemmeltekst),
            søkerNavn = organisasjonsnavn ?: grunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer =
                grunnlagOgSignaturData.grunnlag.søker.aktør
                    .aktivFødselsnummer(),
            perioder = brevperioder,
            organisasjonsnummer = organisasjonsnummer,
            gjelder = if (organisasjonsnummer != null) grunnlagOgSignaturData.grunnlag.søker.navn else null,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) },
            utbetalingerPerMndEøs = utbetalingerPerMndEøs,
        )
    }

    private fun lagVedtaksbrevFellesfelterSammensattKontrollsak(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): VedtakFellesfelterSammensattKontrollsak {
        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak.behandling)

        val organisasjonsnummer =
            vedtak.behandling.fagsak.institusjon
                ?.orgNummer
        val organisasjonsnavn = organisasjonsnummer?.let { organisasjonService.hentOrganisasjon(it).navn }

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)

        val utbetalingerPerMndEøs = hentUtbetalingerPerMndEøs(vedtak)

        return VedtakFellesfelterSammensattKontrollsak(
            enhet = grunnlagOgSignaturData.enhet,
            saksbehandler = grunnlagOgSignaturData.saksbehandler,
            beslutter = grunnlagOgSignaturData.beslutter,
            søkerNavn = organisasjonsnavn ?: grunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer =
                grunnlagOgSignaturData.grunnlag.søker.aktør
                    .aktivFødselsnummer(),
            organisasjonsnummer = organisasjonsnummer,
            gjelder = if (organisasjonsnummer != null) grunnlagOgSignaturData.grunnlag.søker.navn else null,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) },
            utbetalingerPerMndEøs = utbetalingerPerMndEøs,
            sammensattKontrollsakFritekst = sammensattKontrollsak.fritekst,
        )
    }

    private fun hentAktivtPersonopplysningsgrunnlag(behandlingId: Long) = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

    private fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? = hentEtterbetalingsbeløp(vedtak)?.let { Etterbetaling(it) }

    private fun hentEtterbetalingInstitusjon(vedtak: Vedtak): EtterbetalingInstitusjon? = hentEtterbetalingsbeløp(vedtak)?.let { EtterbetalingInstitusjon(it) }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak): String? {
        val etterbetalingsBeløp =
            korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(vedtak.behandling.id)?.beløp?.toBigDecimal()
                ?: simuleringService.hentEtterbetaling(vedtak.behandling.id)

        return etterbetalingsBeløp.takeIf { it > BigDecimal.ZERO }?.run { Utils.formaterBeløp(this.toInt()) }
    }

    private fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean = simuleringService.hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    private fun hentGrunnlagOgSignaturData(behandling: Behandling): GrunnlagOgSignaturData {
        val personopplysningGrunnlag = hentAktivtPersonopplysningsgrunnlag(behandling.id)
        val (saksbehandler, beslutter) =
            hentSaksbehandlerOgBeslutter(
                behandling = behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id),
            )

        val enhet =
            if (behandling.skalBehandlesAutomatisk) {
                AUTOMATISK_BEHANDLING_BREVSIGNATUR
            } else {
                arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn
            }

        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            enhet = enhet,
        )
    }

    fun finnStarttidspunktForUtbetalingstabell(behandling: Behandling): LocalDate {
        val førsteJanuarIFjor = LocalDate.now().minusYears(1).withDayOfYear(1)
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling.id)

        return when {
            behandling.opprettetÅrsak != BehandlingÅrsak.ÅRLIG_KONTROLL || endringstidspunkt.isBefore(førsteJanuarIFjor) -> {
                endringstidspunkt
            }

            else -> {
                val endretutbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandling.id)
                val tidligsteUtbetaling =
                    andelTilkjentYtelseRepository
                        .finnAndelerTilkjentYtelseForBehandling(behandling.id)
                        .filtrerBortIrrelevanteAndeler(endretutbetalingAndeler)
                        .minOfOrNull { it.stønadFom }
                        ?.toLocalDate() ?: return TIDENES_ENDE

                tidligsteUtbetaling.coerceAtLeast(førsteJanuarIFjor)
            }
        }
    }

    private fun hentUtbetalingerPerMndEøs(
        vedtak: Vedtak,
    ): Map<String, UtbetalingMndEøs>? {
        val behandlingId = vedtak.behandling.id
        val endringstidspunkt = finnStarttidspunktForUtbetalingstabell(behandling = vedtak.behandling)
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId = behandlingId)
        val endretutbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandlingId)

        if (!skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser)) {
            return null
        }

        val andelerTilkjentYtelseForBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandlingId).toList()

        return hentUtbetalingerPerMndEøs(
            endringstidspunkt = endringstidspunkt,
            andelTilkjentYtelserForBehandling = andelerTilkjentYtelseForBehandling,
            utenlandskePeriodebeløp = utenlandskePeriodebeløp,
            valutakurser = valutakurser,
            endretutbetalingAndeler = endretutbetalingAndeler,
        )
    }

    fun hentSaksbehandlerOgBeslutter(
        behandling: Behandling,
        totrinnskontroll: Totrinnskontroll?,
    ): Pair<String, String> =
        when {
            behandling.steg <= StegType.SEND_TIL_BESLUTTER || totrinnskontroll == null -> {
                Pair(saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(), "Beslutter")
            }

            totrinnskontroll.erBesluttet() -> {
                Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter!!)
            }

            behandling.steg == StegType.BESLUTTE_VEDTAK -> {
                Pair(
                    totrinnskontroll.saksbehandler,
                    if (totrinnskontroll.saksbehandler == saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()) {
                        "Beslutter"
                    } else {
                        saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()
                    },
                )
            }

            else -> {
                throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
            }
        }

    fun hentBrevForTilbakekrevingsvedtakMotregning(tilbakekrevingsvedtakMotregning: TilbakekrevingsvedtakMotregning): TilbakekrevingsvedtakMotregningBrev {
        val behandling = tilbakekrevingsvedtakMotregning.behandling
        val fagsak = behandling.fagsak

        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(behandling)

        val mottakerIdent = fagsak.institusjon?.orgNummer ?: fagsak.aktør.aktivFødselsnummer()
        val navn = grunnlagOgSignaturData.grunnlag.søker.navn
        val enhet = grunnlagOgSignaturData.enhet

        val avregningperioderForBehandling = avregningService.hentPerioderMedAvregning(behandling.id)

        val avregningperioderBeskrevet =
            avregningperioderForBehandling.map { (fom, tom) ->
                if (fom.toYearMonth() == tom.toYearMonth()) {
                    fom.tilMånedÅr()
                } else {
                    "${fom.tilMånedÅr()} til og med ${tom.tilMånedÅr()}"
                }
            }

        return TilbakekrevingsvedtakMotregningBrev(
            data =
                TilbakekrevingsvedtakMotregningBrevData(
                    delmalData =
                        TilbakekrevingsvedtakMotregningBrevData.DelmalData(
                            signaturVedtak =
                                SignaturVedtak(
                                    enhet = enhet,
                                    saksbehandler = grunnlagOgSignaturData.saksbehandler,
                                    beslutter = grunnlagOgSignaturData.beslutter,
                                ),
                        ),
                    flettefelter =
                        TilbakekrevingsvedtakMotregningBrevData.Flettefelter(
                            navn = navn,
                            fodselsnummer = mottakerIdent,
                            brevOpprettetDato = LocalDate.now(),
                            tilbakekrevingsvedtakMotregning = tilbakekrevingsvedtakMotregning,
                            sumAvFeilutbetaling = Utils.formaterBeløp(avregningperioderForBehandling.sumOf { it.totalFeilutbetaling }.toInt()),
                            avregningperioder = avregningperioderBeskrevet,
                        ),
                ),
            mal = Brevmal.TILBAKEKREVINGSVEDTAK_MOTREGNING,
        )
    }

    private data class GrunnlagOgSignaturData(
        val grunnlag: PersonopplysningGrunnlag,
        val saksbehandler: String,
        val beslutter: String,
        val enhet: String,
    )
}
