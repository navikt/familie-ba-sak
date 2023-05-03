package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.BRUKE_TIDSLINJE_I_STEDET_FOR
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestGenererVedtaksperioderForOverstyrtEndringstidspunkt
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.EndringstidspunktService
import no.nav.familie.ba.sak.kjerne.beregning.endringstidspunkt.filtrerLikEllerEtterEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.hentIPeriode
import no.nav.familie.ba.sak.kjerne.brev.hentKompetanserSomStopperRettFørPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform.NB
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform.NN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValutaRepository
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
    private val personidentService: PersonidentService,
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val endringstidspunktService: EndringstidspunktService,
    private val utbetalingsperiodeMedBegrunnelserService: UtbetalingsperiodeMedBegrunnelserService,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val featureToggleService: FeatureToggleService,
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,
    private val brevmalService: BrevmalService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val refusjonEøsRepository: RefusjonEøsRepository,
    private val integrasjonClient: IntegrasjonClient
) {
    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            restPutVedtaksperiodeMedFritekster.fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it
                )
            }
        )

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        standardbegrunnelserFraFrontend: List<Standardbegrunnelse>,
        eøsStandardbegrunnelserFraFrontend: List<EØSStandardbegrunnelse> = emptyList()
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            standardbegrunnelserFraFrontend.mapNotNull {
                val triggesAv = it.tilISanityBegrunnelse(sanityBegrunnelser)?.tilTriggesAv()
                    ?: return@mapNotNull null

                if (triggesAv.satsendring) {
                    validerSatsendring(
                        fom = vedtaksperiodeMedBegrunnelser.fom,
                        harBarnMedSeksårsdagPåFom = persongrunnlag.harBarnMedSeksårsdagPåFom(
                            vedtaksperiodeMedBegrunnelser.fom
                        )
                    )
                }

                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            }
        )

        vedtaksperiodeMedBegrunnelser.settEØSBegrunnelser(
            eøsStandardbegrunnelserFraFrontend.map {
                EØSBegrunnelse(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    begrunnelse = it
                )
            }
        )

        if (
            standardbegrunnelserFraFrontend.any { it.vedtakBegrunnelseType == VedtakBegrunnelseType.ENDRET_UTBETALING }
        ) {
            val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    private fun validerEndretUtbetalingsbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        persongrunnlag: PersonopplysningGrunnlag
    ) {
        try {
            vedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag
            )
        } catch (e: Exception) {
            throw FunksjonellFeil(
                "Begrunnelse for endret utbetaling er ikke gyldig for vedtaksperioden"
            )
        }
    }

    fun oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak: Vedtak, barnaSomVurderes: List<String>) {
        val barnaAktørSomVurderes = personidentService.hentAktørIder(barnaSomVurderes)

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)
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
                    vilkårsvurderingService.hentAktivForBehandling(behandlingId = vedtak.behandling.id)
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
                        standardbegrunnelse = if (vedtak.behandling.fagsak.status == FagsakStatus.LØPENDE) {
                            Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
                        } else {
                            Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE
                        },
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser
                    )
                )
            )
            vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

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
                                    standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                                    vedtaksperiodeMedBegrunnelser = satsendringsvedtaksperiode
                                )
                            )
                        )
                        vedtaksperiodeHentOgPersisterService.lagre(satsendringsvedtaksperiode)
                    }
            }
        }
    }

    @Transactional
    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
        val behandling = vedtak.behandling

        // Rent fortsatt innvilget-resultat er det eneste som kun skal gi én vedtaksperiode
        if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
            val vedtaksbrevmal = brevmalService.hentVedtaksbrevmal(
                behandling
            )

            val erAutobrevFor6Og18ÅrOgSmåbarnstillegg =
                vedtaksbrevmal == Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG

            val fom = if (erAutobrevFor6Og18ÅrOgSmåbarnstillegg) {
                YearMonth.now().førsteDagIInneværendeMåned()
            } else {
                null
            }

            val tom = if (erAutobrevFor6Og18ÅrOgSmåbarnstillegg) {
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandling.id)
            } else {
                null
            }

            vedtaksperiodeHentOgPersisterService.lagre(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom,
                    tom = tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
                )
            )
        } else {
            vedtaksperiodeHentOgPersisterService.lagre(
                if (featureToggleService.isEnabled(FeatureToggleConfig.VEDTAKSPERIODE_NY)) {
                    finnVedtaksperioderForBehandling(vedtak.behandling.id)
                } else {
                    genererVedtaksperioderMedBegrunnelserGammel(vedtak)
                }
            )
        }
    }

    fun finnVedtaksperioderForBehandling(behandlingId: Long): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        val endringstidspunkt = behandling.overstyrtEndringstidspunkt
            ?: endringstidspunktService.finnEndringstidspunktForBehandling(behandlingId = behandling.id)

        return genererVedtaksperioder(
            grunnlagForVedtakPerioder = hentGrunnlagForVedtaksperioder(behandling),
            grunnlagForVedtakPerioderForrigeBehandling = forrigeBehandling?.let { hentGrunnlagForVedtaksperioder(it) },
            vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
        ).filtrerLikEllerEtterEndringstidspunkt(endringstidspunkt)
    }

    fun hentGrunnlagForVedtaksperioder(behandling: Behandling): GrunnlagForVedtaksperioder =
        GrunnlagForVedtaksperioder(
            persongrunnlag = persongrunnlagService.hentAktivThrows(behandling.id),
            personResultater = vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id).personResultater,
            fagsakType = behandling.fagsak.type,
            kompetanser = kompetanseRepository.finnFraBehandlingId(behandling.id).toList(),
            endredeUtbetalinger = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id),
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id),
            perioderOvergangsstønad = småbarnstilleggService.hentPerioderMedFullOvergangsstønad(behandling.id)
        )

    @Deprecated("skal bruke genererVedtaksperioderMedBegrunnelser når den er klar")
    fun genererVedtaksperioderMedBegrunnelserGammel(
        vedtak: Vedtak,
        manueltOverstyrtEndringstidspunkt: LocalDate? = null
    ): List<VedtaksperiodeMedBegrunnelser> {
        /**
         * Hvis endringstidspunktet er overskrevet av saksbehandler skal man bruke det saksbehandler har valgt
         * Hvis toggle for behandlingsresultat er AV og man ønsker å ha fortsatt innvilget MED perioder:
         *         - alle perioder skal med -> endringstidspunkt = tidenes morgen
         * Hvis toggle for behandlingsresultat er PÅ og behandlingsresultat = endret og fortsatt innvilget (betyr i praksis det samme som den over, fordi med toggle på oppfører "endret og fortsatt innvilget" seg likt som fortsatt innvilget med perioder )
         *         - alle perioder skal med -> endringstidspunkt = tidenes morgen
         * Ellers: endringstidspunkt skal utledes
         **/
        val endringstidspunkt = manueltOverstyrtEndringstidspunkt
            ?: endringstidspunktService.finnEndringstidspunktForBehandling(behandlingId = vedtak.behandling.id)

        val opphørsperioder: List<VedtaksperiodeMedBegrunnelser> =
            hentOpphørsperioder(vedtak.behandling, endringstidspunkt).map {
                it.tilVedtaksperiodeMedBegrunnelse(
                    vedtak
                )
            }

        val utbetalingsperioder: List<VedtaksperiodeMedBegrunnelser> =
            utbetalingsperiodeMedBegrunnelserService.hentUtbetalingsperioder(vedtak, opphørsperioder)

        val avslagsperioder = hentAvslagsperioderMedBegrunnelser(vedtak)

        return (utbetalingsperioder + opphørsperioder).filtrerLikEllerEtterEndringstidspunkt(
            endringstidspunkt = endringstidspunkt
        ) + avslagsperioder
    }

    @Transactional
    fun genererVedtaksperiodeForOverstyrtEndringstidspunkt(
        restGenererVedtaksperioder: RestGenererVedtaksperioderForOverstyrtEndringstidspunkt
    ) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(restGenererVedtaksperioder.behandlingId)
        val overstyrtEndringstidspunkt = restGenererVedtaksperioder.overstyrtEndringstidspunkt
        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
            oppdaterVedtakMedVedtaksperioder(vedtak)
        } else {
            vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)
            val vedtaksperioder = if (featureToggleService.isEnabled(FeatureToggleConfig.VEDTAKSPERIODE_NY)) {
                finnVedtaksperioderForBehandling(vedtak.behandling.id)
            } else {
                genererVedtaksperioderMedBegrunnelserGammel(
                    vedtak = vedtak,
                    manueltOverstyrtEndringstidspunkt = overstyrtEndringstidspunkt
                )
            }
            vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder.sortedBy { it.fom })
        }
        lagreNedOverstyrtEndringstidspunkt(vedtak.behandling.id, overstyrtEndringstidspunkt)
    }

    private fun lagreNedOverstyrtEndringstidspunkt(behandlingId: Long, overstyrtEndringstidspunkt: LocalDate) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        behandling.overstyrtEndringstidspunkt = overstyrtEndringstidspunkt
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling = behandling, sendTilDvh = false)
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser = vedtaksperiodeHentOgPersisterService.lagre(
                VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type
                )
            )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )
            nyVedtaksperiodeMedBegrunnelser.settEØSBegrunnelser(
                vedtaksperiodeMedBegrunnelser.eøsBegrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )

            vedtaksperiodeHentOgPersisterService.lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling

        val endreteUtbetalinger = endretUtbetalingAndelRepository.findByBehandlingId(behandling.id)

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val persongrunnlag =
            persongrunnlagService.hentAktivThrows(behandling.id)

        val utvidetVedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag
            )
        }

        val skalSendeMedGyldigeBegrunnelser =
            behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

        return if (skalSendeMedGyldigeBegrunnelser) {
            hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
                behandling = behandling,
                utvidedeVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
                persongrunnlag = persongrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                endretUtbetalingAndeler = endreteUtbetalinger
            )
        } else {
            utvidetVedtaksperioderMedBegrunnelser
        }
    }

    private fun hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
        behandling: Behandling,
        utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        persongrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val sanityEØSBegrunnelser = sanityService.hentSanityEØSBegrunnelser()
        val kompetanser = kompetanseRepository.finnFraBehandlingId(behandling.id)

        return utvidedeVedtaksperioderMedBegrunnelser.map { utvidetVedtaksperiodeMedBegrunnelser ->
            val kompetanserIPeriode = kompetanser.hentIPeriode(
                fom = utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth(),
                tom = utvidetVedtaksperiodeMedBegrunnelser.tom?.toYearMonth()
            ).toList()

            val kompetanserSomStopperRettFørPeriode = hentKompetanserSomStopperRettFørPeriode(
                kompetanser = kompetanser.toList(),
                periodeFom = utvidetVedtaksperiodeMedBegrunnelser.fom?.toYearMonth()
            )

            val aktørIderMedUtbetaling =
                hentAktørerMedUtbetaling(utvidetVedtaksperiodeMedBegrunnelser, persongrunnlag).map { it.aktørId }

            utvidetVedtaksperiodeMedBegrunnelser.copy(
                gyldigeBegrunnelser = hentGyldigeBegrunnelserForPeriode(
                    utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                    sanityBegrunnelser = sanityBegrunnelser,
                    persongrunnlag = persongrunnlag,
                    vilkårsvurdering = vilkårsvurdering,
                    aktørIderMedUtbetaling = aktørIderMedUtbetaling,
                    endretUtbetalingAndeler = endretUtbetalingAndeler,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    sanityEØSBegrunnelser = sanityEØSBegrunnelser,
                    kompetanserIPeriode = kompetanserIPeriode,
                    kompetanserSomStopperRettFørPeriode = kompetanserSomStopperRettFørPeriode
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
        standardbegrunnelse: Standardbegrunnelse
    ) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
            vedtaksperioder.singleOrNull()
                ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        fortsattInnvilgetPeriode.settBegrunnelser(
            listOf(
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                    standardbegrunnelse = standardbegrunnelse
                )
            )
        )

        vedtaksperiodeHentOgPersisterService.lagre(fortsattInnvilgetPeriode)
    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
            .filter { it.stønadFom <= YearMonth.now() && it.stønadTom >= YearMonth.now() }
            .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()
            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hentUtbetalingsperioder(
        behandling: Behandling
    ): List<Utbetalingsperiode> {
        val personopplysningGrunnlag =
            persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                ?: return emptyList()

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        if (featureToggleService.isEnabled(BRUKE_TIDSLINJE_I_STEDET_FOR)) {
            return andelerTilkjentYtelse.mapTilUtbetalingsperioder(
                personopplysningGrunnlag = personopplysningGrunnlag
            )
        } else {
            return mapTilUtbetalingsperioderGammel(personopplysningGrunnlag, andelerTilkjentYtelse)
        }
    }

    fun hentOpphørsperioder(
        behandling: Behandling,
        endringstidspunkt: LocalDate = TIDENES_MORGEN
    ): List<Opphørsperiode> {
        if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) return emptyList()

        val sisteVedtattBehandling: Behandling? =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
            if (sisteVedtattBehandling != null) {
                persongrunnlagService.hentAktiv(behandlingId = sisteVedtattBehandling.id)
            } else {
                null
            }
        val forrigeAndelerMedEndringer = if (sisteVedtattBehandling != null) {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteVedtattBehandling.id)
        } else {
            emptyList()
        }

        val personopplysningGrunnlag =
            persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                ?: return emptyList()

        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        return if (featureToggleService.isEnabled(BRUKE_TIDSLINJE_I_STEDET_FOR)) {
            val alleOpphørsperioder = mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerMedEndringer,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
            )

            val (perioderFørEndringstidspunkt, fraEndringstidspunktOgUtover) =
                alleOpphørsperioder.partition { it.periodeFom.isBefore(endringstidspunkt) }

            perioderFørEndringstidspunkt + slåSammenOpphørsperioder(fraEndringstidspunktOgUtover)
        } else {
            val alleOpphørsperioder = mapTilOpphørsperioderGammel(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerMedEndringer,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
            )

            val (perioderFørEndringstidspunkt, fraEndringstidspunktOgUtover) =
                alleOpphørsperioder.partition { it.periodeFom.isBefore(endringstidspunkt) }

            perioderFørEndringstidspunkt + slåSammenOpphørsperioderGammel(fraEndringstidspunktOgUtover)
        }
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
            vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        val avslagsperioder = periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val standardbegrunnelser =
                vilkårResultater.map { it.standardbegrunnelser }.flatten().toSet().toList()

            val nasjonaleStandardbegrunnelser = standardbegrunnelser.filterIsInstance<Standardbegrunnelse>()
            val eøsStandardbegrunnelser = standardbegrunnelser.filterIsInstance<EØSStandardbegrunnelse>()

            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fellesPeriode.fom,
                tom = fellesPeriode.tom,
                type = Vedtaksperiodetype.AVSLAG
            )
                .apply {
                    begrunnelser.addAll(
                        nasjonaleStandardbegrunnelser.map { begrunnelse ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                standardbegrunnelse = begrunnelse
                            )
                        }
                    )
                    eøsBegrunnelser.addAll(
                        eøsStandardbegrunnelser.map { begrunnelse ->
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                begrunnelse = begrunnelse
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
        } else {
            avslagsperioder
        }
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
            } else {
                avslagsperioder
            }

        return avslagsperioderMedTomPeriode.map {
            if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
                it.apply {
                    when (vedtak.behandling.kategori) {
                        BehandlingKategori.NASJONAL -> begrunnelser.add(
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                standardbegrunnelse = Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN
                            )
                        )

                        BehandlingKategori.EØS -> eøsBegrunnelser.add(
                            EØSBegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                begrunnelse = EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN
                            )
                        )
                    }
                }
            } else {
                it
            }
        }.toList()
    }

    fun skalHaÅrligKontroll(vedtak: Vedtak): Boolean {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.EØS_INFORMASJON_OM_ÅRLIG_KONTROLL, false)) {
            return false
        }
        return vedtak.behandling.kategori == BehandlingKategori.EØS &&
            hentPersisterteVedtaksperioder(vedtak).any { it.tom?.erSenereEnnInneværendeMåned() != false }
    }

    fun beskrivPerioderMedFeilutbetaltValuta(vedtak: Vedtak): Set<String>? {
        val målform = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)?.søker?.målform
        val fra = mapOf(NB to "Fra", NN to "Frå").getOrDefault(målform, "Fra")
        val mye = mapOf(NB to "mye", NN to "mykje").getOrDefault(målform, "mye")

        return feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(vedtak.behandling.id).map {
            val (fom, tom) = it.fom.tilDagMånedÅr() to it.tom.tilDagMånedÅr()
            "$fra $fom til $tom er det utbetalt ${it.feilutbetaltBeløp} kroner for $mye."
        }.toSet().takeIf { it.isNotEmpty() }
    }

    fun beskrivPerioderMedRefusjonEøs(behandling: Behandling, avklart: Boolean): Set<String>? {
        val målform = persongrunnlagService.hentAktiv(behandlingId = behandling.id)?.søker?.målform
        val landkoderISO2 = integrasjonClient.hentLandkoderISO2()

        return refusjonEøsRepository.finnRefusjonEøsForBehandling(behandling.id).filter { it.refusjonAvklart == avklart }.map {
            val (fom, tom) = it.fom.tilDagMånedÅr() to it.tom.tilDagMånedÅr()
            val land = landkoderISO2[it.land]?.storForbokstav() ?: throw Feil("Fant ikke navn for landkode ${it.land} ")
            val beløp = it.refusjonsbeløp

            when (målform) {
                NN -> {
                    if (avklart) {
                        "Frå $fom til $tom blir $beløp kroner av etterbetalinga di utbetalt til myndighetene i $land"
                    } else {
                        "Fra $fom til $tom blir ikkje etterbetalinga på $beløp kroner utbetalt no sidan det er utbetalt barnetrygd i $land"
                    }
                }
                else -> {
                    if (avklart) {
                        "Fra $fom til $tom blir $beløp kroner av etterbetalingen din utbetalt til myndighetene i $land"
                    } else {
                        "Fra $fom til $tom blir ikke etterbetalingen på ${it.refusjonsbeløp} kroner utbetalt nå siden det er utbetalt barnetrygd i $land"
                    }
                }
            }
        }.toSet().takeIf { it.isNotEmpty() }
    }

    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
