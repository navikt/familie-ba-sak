package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.FØRSTE_ENDRINGSTIDSPUNKT
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.INGEN_OVERLAPP_VEDTAKSPERIODER
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.EndringstidspunktSerivce
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.hentVedtaksbrevmal
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
    private val behandlingRepository: BehandlingRepository,
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val endringstidspunktSerivce: EndringstidspunktSerivce,
    private val featureToggleService: FeatureToggleService,
) {

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        validerVedtaksperiodeMedBegrunnelser(vedtaksperiodeMedBegrunnelser)
        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun lagre(vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): List<VedtaksperiodeMedBegrunnelser> =
        vedtaksperiodeRepository.saveAll(vedtaksperiodeMedBegrunnelser)

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            restPutVedtaksperiodeMedFritekster.fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it
                )
            }
        )

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        standardbegrunnelserFraFrontend: List<VedtakBegrunnelseSpesifikasjon>
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val erIngenOverlappVedtaksperiodeToggelPå = featureToggleService.isEnabled(INGEN_OVERLAPP_VEDTAKSPERIODER)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            standardbegrunnelserFraFrontend.mapNotNull {

                val triggesAv = it.tilSanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()
                    ?: return@mapNotNull null

                if (triggesAv.satsendring) {
                    validerSatsendring(
                        fom = vedtaksperiodeMedBegrunnelser.fom,
                        harBarnMedSeksårsdagPåFom = persongrunnlag.harBarnMedSeksårsdagPåFom(
                            vedtaksperiodeMedBegrunnelser.fom
                        )
                    )
                }

                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser, erIngenOverlappVedtaksperiodeToggelPå)
            }
        )

        if (
            standardbegrunnelserFraFrontend.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING } &&
            !featureToggleService.isEnabled(INGEN_OVERLAPP_VEDTAKSPERIODER)
        ) {
            val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    private fun validerEndretUtbetalingsbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        persongrunnlag: PersonopplysningGrunnlag
    ) {
        try {
            vedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag,
                erIngenOverlappVedtaksperiodeTogglePå = false,
            )
        } catch (e: Exception) {
            throw FunksjonellFeil(
                "Begrunnelse for endret utbetaling er ikke gyldig for vedtaksperioden"
            )
        }
    }

    fun oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak: Vedtak, barnaSomVurderes: List<String>) {
        val barnaAktørSomVurderes = personidentService.hentAktørIder(barnaSomVurderes)

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = vedtak.behandling.id)
        val vurderteBarnSomPersoner =
            barnaAktørSomVurderes.map { barnAktørSomVurderes ->
                persongrunnlag.barna.find { it.aktør == barnAktørSomVurderes }
                    ?: error("Finner ikke barn som har blitt vurdert i persongrunnlaget")
            }

        vurderteBarnSomPersoner.map { it.fødselsdato.toYearMonth() }.toSet().forEach { fødselsmåned ->
            val vedtaksperiodeMedBegrunnelser = vedtaksperioderMedBegrunnelser.firstOrNull {
                fødselsmåned.plusMonths(1).equals(it.fom?.toYearMonth() ?: TIDENES_ENDE)
            }

            if (vedtaksperiodeMedBegrunnelser == null) {
                val vilkårsvurdering =
                    vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = vedtak.behandling.id)
                secureLogger.info(
                    vilkårsvurdering?.personResultater?.map {
                        "Fødselsnummer: ${it.aktør.aktivFødselsnummer()}.  Resultater: ${it.vilkårResultater}"
                    }?.joinToString("\n")
                )
                throw Feil("Finner ikke vedtaksperiode å begrunne for barn fra hendelse")
            }

            vedtaksperiodeMedBegrunnelser.settBegrunnelser(
                listOf(
                    Vedtaksbegrunnelse(
                        vedtakBegrunnelseSpesifikasjon = if (vedtak.behandling.fagsak.status == FagsakStatus.LØPENDE) {
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
                        } else VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE,
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    )
                )
            )
            lagre(vedtaksperiodeMedBegrunnelser)

            /**
             * Hvis barn(a) er født før desember påvirkes vedtaket av satsendring januar 2022
             * og vi må derfor også automatisk begrunne satsendringen
             */
            if (fødselsmåned < YearMonth.of(
                    2021,
                    12
                )
            ) {
                vedtaksperioderMedBegrunnelser.firstOrNull { it.fom?.toYearMonth() == YearMonth.of(2022, 1) }
                    ?.also { satsendringsvedtaksperiode ->
                        satsendringsvedtaksperiode.settBegrunnelser(
                            listOf(
                                Vedtaksbegrunnelse(
                                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING,
                                    vedtaksperiodeMedBegrunnelser = satsendringsvedtaksperiode,
                                )
                            )
                        )
                        lagre(satsendringsvedtaksperiode)
                    }
            }
        }
    }

    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {

        slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {

            val vedtaksbrevmal = hentVedtaksbrevmal(vedtak.behandling)
            val erAutobrevFor6Og18ÅrOgSmåbarnstillegg =
                vedtaksbrevmal == Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG

            val fom = if (erAutobrevFor6Og18ÅrOgSmåbarnstillegg) {
                YearMonth.now().førsteDagIInneværendeMåned()
            } else null

            val tom = if (erAutobrevFor6Og18ÅrOgSmåbarnstillegg) {
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(vedtak.behandling.id)
            } else null

            lagre(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom,
                    tom = tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
                )
            )
        } else {
            lagre(genererVedtaksperioderMedBegrunnelser(vedtak))
        }
    }

    fun genererVedtaksperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)

        val erIngenOverlappVedtaksperiodeTogglePå = featureToggleService.isEnabled(INGEN_OVERLAPP_VEDTAKSPERIODER)

        val utbetalingsperioder = if (!erIngenOverlappVedtaksperiodeTogglePå) {
            hentVedtaksperioderMedBegrunnelserForUtbetalingsperioderGammel(
                andelerTilkjentYtelse,
                vedtak
            )
        } else hentVedtaksperioderMedBegrunnelserForUtbetalingsperioder(
            andelerTilkjentYtelse,
            vedtak
        )

        val endredeUtbetalingsperioder =
            if (!erIngenOverlappVedtaksperiodeTogglePå) hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                vedtak = vedtak,
            ) else emptyList()

        val opphørsperioder =
            hentOpphørsperioder(vedtak.behandling).map { it.tilVedtaksperiodeMedBegrunnelse(vedtak) }

        val avslagsperioder = hentAvslagsperioderMedBegrunnelser(vedtak)

        val reduksjonsperioder = hentReduksjonsperioder(vedtak, opphørsperioder)

        val oppdatertUtbetalingsperiode =
            finnOgOppdaterOverlappendeUtbetalingsperiode(utbetalingsperioder, reduksjonsperioder)

        val endringstidspunkt =
            if (featureToggleService.isEnabled(FØRSTE_ENDRINGSTIDSPUNKT))
                endringstidspunktSerivce.finnEndringstidpunkForBehandling(behandlingId = vedtak.behandling.id)
            else TIDENES_MORGEN

        return (oppdatertUtbetalingsperiode + endredeUtbetalingsperioder + opphørsperioder + avslagsperioder).filter {
            (it.tom ?: TIDENES_ENDE).isSameOrAfter(endringstidspunkt)
        }
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser = lagre(
                VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type,
                )
            )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )

            lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
            behandlingId = behandling.id
        )
        val persongrunnlag =
            persongrunnlagService.hentAktivThrows(behandling.id)

        val erIngenOverlappVedtaksperiodeTogglePå = featureToggleService.isEnabled(INGEN_OVERLAPP_VEDTAKSPERIODER)

        val utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag,
                erIngenOverlappVedtaksperiodeTogglePå = erIngenOverlappVedtaksperiodeTogglePå
            )
        }

        val skalSendeMedGyldigeBegrunnelser =
            behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

        return if (skalSendeMedGyldigeBegrunnelser) {
            hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
                behandling = behandling,
                utvidedeVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
                persongrunnlag = persongrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
            )
        } else utvidetVedtaksperioderMedBegrunnelser
    }

    private fun hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
        behandling: Behandling,
        utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        persongrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(
            behandling.id
        )

        val erIngenOverlappVedtaksperiodeToggelPå = featureToggleService.isEnabled(INGEN_OVERLAPP_VEDTAKSPERIODER)

        return utvidedeVedtaksperioderMedBegrunnelser.map { utvidetVedtaksperiodeMedBegrunnelser ->
            val aktørIderMedUtbetaling =
                hentAktørerMedUtbetaling(utvidetVedtaksperiodeMedBegrunnelser, persongrunnlag).map { it.aktørId }

            utvidetVedtaksperiodeMedBegrunnelser.copy(
                gyldigeBegrunnelser = hentGyldigeBegrunnelserForVedtaksperiode(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    persongrunnlag = persongrunnlag,
                    vilkårsvurdering = vilkårsvurdering,
                    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                    endretUtbetalingAndeler = endretUtbetalingAndeler,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    erIngenOverlappVedtaksperiodeToggelPå = erIngenOverlappVedtaksperiodeToggelPå
                )
            )
        }
    }

    private fun hentAktørerMedUtbetaling(
        utvidetVedtaksperiodeMedBegrunnelser: UtvidetVedtaksperiodeMedBegrunnelser,
        persongrunnlag: PersonopplysningGrunnlag
    ): List<Aktør> = utvidetVedtaksperiodeMedBegrunnelser
        .utbetalingsperiodeDetaljer
        .map { utbetalingsperiodeDetalj ->
            val ident = utbetalingsperiodeDetalj.person.personIdent
            persongrunnlag.personer.find { it.aktør.aktivFødselsnummer() == ident }?.aktør
                ?: personidentService.hentAktør(ident)
        }

    fun oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
        vedtak: Vedtak,
        vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon
    ) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
            vedtaksperioder.singleOrNull()
                ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        fortsattInnvilgetPeriode.settBegrunnelser(
            listOf(
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                )
            )
        )

        lagre(fortsattInnvilgetPeriode)
    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
            .filter { it.stønadFom <= YearMonth.now() && it.stønadTom >= YearMonth.now() }
            .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()
            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hentUtbetalingsperioder(
        behandling: Behandling,
    ): List<Utbetalingsperiode> {
        val personopplysningGrunnlag =
            persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                ?: return emptyList()
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilUtbetalingsperioder(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
        )
    }

    fun hentOpphørsperioder(behandling: Behandling): List<Opphørsperiode> {
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
            behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
            iverksatteBehandlinger = iverksatteBehandlinger,
            behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
            if (forrigeIverksatteBehandling != null)
                persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id)
            else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            if (forrigeIverksatteBehandling != null)
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                    behandlingId = forrigeIverksatteBehandling.id
                ) else emptyList()

        val personopplysningGrunnlag =
            persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                ?: return emptyList()
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilOpphørsperioder(
            forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }

    fun hentReduksjonsperioder(
        vedtak: Vedtak,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        if (behandling.skalBehandlesAutomatisk) return emptyList()
        val forrigeIverksatteBehandling: Behandling = hentForrigeIverksatteBehandling(behandling) ?: return emptyList()
        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag =
            forrigeIverksatteBehandling.let { persongrunnlagService.hentAktivThrows(it.id) }

        val forrigeAndelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeIverksatteBehandling.id)
        // henter andel tilkjent ytelse for barn som finnes i forrige behandling
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
            .filter {
                forrigePersonopplysningGrunnlag.søkerOgBarn.any { f -> f.aktør == it.aktør }
            }

        val forrigeSegmenter = forrigeAndelerTilkjentYtelse.lagVertikaleSegmenter().keys
        val nåværendeSegmenter = andelerTilkjentYtelse.lagVertikaleSegmenter().keys
        val segmenter = forrigeSegmenter.filterNot {
            nåværendeSegmenter.any { seg -> it.fom == seg.fom && it.tom == seg.tom && it.value == seg.value }
        }
        val reduksjonsperioder = mutableListOf<VedtaksperiodeMedBegrunnelser>()
        segmenter.filter { nåværendeSegmenter.any { seg -> seg.overlapper(it) } }
            .forEach {
                val overlappendePerioder =
                    nåværendeSegmenter.filter { nåSegment -> nåSegment.overlapper(it) && nåSegment.value < it.value }
                overlappendePerioder.forEach { overlappendePeriode ->
                    reduksjonsperioder.add(
                        VedtaksperiodeMedBegrunnelser(
                            vedtak = vedtak,
                            fom = if (it.fom > overlappendePeriode.fom) it.fom else overlappendePeriode.fom,
                            tom = if (it.tom > overlappendePeriode.tom) overlappendePeriode.tom else it.tom,
                            type = Vedtaksperiodetype.REDUKSJON,
                        )
                    )
                }
            }

        // opphørsperioder kan ikke være inkludert i reduksjonsperioder
        return reduksjonsperioder.filterNot { reduksjonsperiode ->
            opphørsperioder.any { it.fom == reduksjonsperiode.fom || it.tom == reduksjonsperiode.tom }
        }
    }

    private fun finnOgOppdaterOverlappendeUtbetalingsperiode(
        utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser>,
        reduksjonsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val overlappendePerioder =
            utbetalingsperioder.filter {
                reduksjonsperioder.any { rd ->
                    rd.fom!!.isSameOrAfter(it.fom!!) && rd.tom!!.isSameOrBefore(
                        it.tom!!
                    )
                }
            }
        val oppdatertUtbetalingsperioder = mutableListOf<VedtaksperiodeMedBegrunnelser>()
        utbetalingsperioder.forEach {
            val overlappendePeriode = overlappendePerioder.firstOrNull { o -> it.fom == o.fom && it.tom == o.tom }
            if (overlappendePeriode != null) {
                oppdatertUtbetalingsperioder.addAll(
                    reduksjonsperioder.filter { r ->
                        r.fom!! >= overlappendePeriode.fom &&
                            r.tom!! <= overlappendePeriode.tom
                    }
                )
            } else {
                oppdatertUtbetalingsperioder.add(it)
            }
        }
        return oppdatertUtbetalingsperioder.sortedBy { it.fom }
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        val vilkårsvurdering =
            vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
                ?: throw Feil(
                    "Fant ikke vilkårsvurdering for behandling ${behandling.id} ved generering av avslagsperioder"
                )

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
            vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        val avslagsperioder = periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val vedtakBegrunnelseSpesifikasjoner =
                vilkårResultater.map { it.vedtakBegrunnelseSpesifikasjoner }.flatten().toSet().toList()

            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fellesPeriode.fom,
                tom = fellesPeriode.tom,
                type = Vedtaksperiodetype.AVSLAG
            )
                .apply {
                    begrunnelser.addAll(
                        vedtakBegrunnelseSpesifikasjoner.map { begrunnelse ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                vedtakBegrunnelseSpesifikasjon = begrunnelse
                            )
                        }
                    )
                }
        }.toMutableList()

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)?.hentUregistrerteBarn()
                ?: emptyList()

        return if (uregistrerteBarn.isNotEmpty()) {
            leggTilAvslagsbegrunnelseForUregistrertBarn(
                avslagsperioder = avslagsperioder,
                vedtak = vedtak,
                uregistrerteBarn = uregistrerteBarn
            )
        } else avslagsperioder
    }

    private fun leggTilAvslagsbegrunnelseForUregistrertBarn(
        avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak,
        uregistrerteBarn: List<BarnMedOpplysninger>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val avslagsperioderMedTomPeriode =
            if (avslagsperioder.none { it.fom == null && it.tom == null }) {
                avslagsperioder + VedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = null,
                    tom = null,
                    type = Vedtaksperiodetype.AVSLAG
                )
            } else avslagsperioder

        return avslagsperioderMedTomPeriode.map {
            if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
                it.apply {
                    begrunnelser.add(
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN,
                        )
                    )
                }
            } else it
        }.toList()
    }

    fun hent(vedtaksperiodeId: Long): VedtaksperiodeMedBegrunnelser =
        vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId)

    private fun hentForrigeIverksatteBehandling(behandling: Behandling): Behandling? {
        val iverksatteBehandlinger =
            behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
            iverksatteBehandlinger = iverksatteBehandlinger,
            behandlingFørFølgende = behandling
        )
        return forrigeIverksatteBehandling
    }

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
