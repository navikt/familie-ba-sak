package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.GenererVedtaksperioderForOverstyrtEndringstidspunktDto
import no.nav.familie.ba.sak.ekstern.restDomene.PutVedtaksperiodeMedFriteksterDto
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform.NB
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform.NN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentUtbetalingsperiodeDetaljer
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta.FeilutbetaltValutaRepository
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelserDto
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.sorter
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelserDto
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.endringstidspunkt.utledEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val overgangsstønadService: OvergangsstønadService,
    private val refusjonEøsRepository: RefusjonEøsRepository,
    private val kodeverkService: KodeverkService,
    private val valutakursRepository: ValutakursRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val featureToggleService: FeatureToggleService,
) {
    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        putVedtaksperiodeMedFriteksterDto: PutVedtaksperiodeMedFriteksterDto,
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)
        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling
        validerBehandlingKanRedigeres(behandling)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            putVedtaksperiodeMedFriteksterDto.fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it,
                )
            },
        )

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun finnEndringstidspunktForBehandling(behandlingId: Long): LocalDate {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        return utledEndringstidspunkt(
            behandlingsGrunnlagForVedtaksperioder = behandling.hentGrunnlagForVedtaksperioder(),
            behandlingsGrunnlagForVedtaksperioderForrigeBehandling = forrigeBehandling?.hentGrunnlagForVedtaksperioder(),
            featureToggleService = featureToggleService,
        )
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        standardbegrunnelserFraFrontend: List<Standardbegrunnelse>,
        eøsStandardbegrunnelserFraFrontend: List<EØSStandardbegrunnelse> = emptyList(),
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId)

        validerAvslagHarAvslagBegrunnelse(
            vedtaksperiodeMedBegrunnelser,
            standardbegrunnelserFraFrontend,
            eøsStandardbegrunnelserFraFrontend,
        )

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling
        validerBehandlingKanRedigeres(behandling)

        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk = true)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            standardbegrunnelserFraFrontend.mapNotNull {
                val sanityBegrunnelse =
                    sanityBegrunnelser[it]
                        ?: return@mapNotNull null

                if (sanityBegrunnelse.øvrigeTriggere.contains(ØvrigTrigger.SATSENDRING)) {
                    validerSatsendring(
                        fom = vedtaksperiodeMedBegrunnelser.fom,
                        harBarnMedSeksårsdagPåFom =
                            persongrunnlag.harBarnMedSeksårsdagPåFom(
                                vedtaksperiodeMedBegrunnelser.fom,
                            ),
                    )
                }

                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            },
        )

        vedtaksperiodeMedBegrunnelser.settEØSBegrunnelser(
            eøsStandardbegrunnelserFraFrontend.map {
                EØSBegrunnelse(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    begrunnelse = it,
                )
            },
        )

        if (
            standardbegrunnelserFraFrontend.any { it.vedtakBegrunnelseType.erEndretUtbetaling() }
        ) {
            val andelerTilkjentYtelse =
                andelerTilkjentYtelseOgEndreteUtbetalingerService
                    .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

            validerEndretUtbetalingsbegrunnelse(vedtaksperiodeMedBegrunnelser, andelerTilkjentYtelse, persongrunnlag)
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    private fun validerAvslagHarAvslagBegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        standardbegrunnelserFraFrontend: List<Standardbegrunnelse>,
        eøsStandardbegrunnelserFraFrontend: List<EØSStandardbegrunnelse>,
    ) {
        val eksisterendeAvslagBegrunnelser =
            vedtaksperiodeMedBegrunnelser.begrunnelser
                .filter { it.standardbegrunnelse.vedtakBegrunnelseType.erAvslag() }
                .map { it.standardbegrunnelse.sanityApiNavn }

        val nyeAvslagBegrunnelser =
            (standardbegrunnelserFraFrontend.filter { it.vedtakBegrunnelseType.erAvslag() } + eøsStandardbegrunnelserFraFrontend.filter { it.vedtakBegrunnelseType.erAvslag() }).map { it.sanityApiNavn }

        if (!nyeAvslagBegrunnelser.containsAll(eksisterendeAvslagBegrunnelser)) {
            throw FunksjonellFeil("Kan ikke fjerne avslags-begrunnelse fra vedtaksperiode som har blitt satt til avslag i vilkårsvurdering.")
        }
    }

    private fun validerEndretUtbetalingsbegrunnelse(
        vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        persongrunnlag: PersonopplysningGrunnlag,
    ) {
        try {
            vedtaksperiodeMedBegrunnelser.hentUtbetalingsperiodeDetaljer(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag,
            )
        } catch (e: Exception) {
            throw FunksjonellFeil(
                "Begrunnelse for endret utbetaling er ikke gyldig for vedtaksperioden",
            )
        }
    }

    @Transactional
    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak)

        val vedtaksperioderForBehandling = finnVedtaksperioderForBehandling(vedtak)
        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioderForBehandling)
    }

    fun finnVedtaksperioderForBehandling(behandlingId: Long): List<VedtaksperiodeMedBegrunnelser> = finnVedtaksperioderForBehandling(vedtakRepository.findByBehandlingAndAktiv(behandlingId))

    fun finnVedtaksperioderForBehandling(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val behandling = vedtak.behandling
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        return genererVedtaksperioder(
            grunnlagForVedtaksperioder = behandling.hentGrunnlagForVedtaksperioder(),
            grunnlagForVedtaksperioderForrigeBehandling = forrigeBehandling?.hentGrunnlagForVedtaksperioder(),
            vedtak = vedtak,
            nåDato = LocalDate.now(),
            featureToggleService = featureToggleService,
        )
    }

    fun Behandling.hentGrunnlagForVedtaksperioder(): BehandlingsGrunnlagForVedtaksperioder =
        BehandlingsGrunnlagForVedtaksperioder(
            persongrunnlag = persongrunnlagService.hentAktivThrows(this.id),
            personResultater = vilkårsvurderingService.hentAktivForBehandling(this.id)?.personResultater ?: emptySet(),
            behandling = this,
            kompetanser = kompetanseRepository.finnFraBehandlingId(this.id).toList(),
            valutakurs = valutakursRepository.finnFraBehandlingId(this.id).toList(),
            utenlandskPeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(this.id).toList(),
            endredeUtbetalinger = endretUtbetalingAndelRepository.findByBehandlingId(this.id),
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(this.id),
            perioderOvergangsstønad = overgangsstønadService.hentPerioderMedFullOvergangsstønad(this),
            uregistrerteBarn =
                søknadGrunnlagService.hentAktiv(behandlingId = this.id)?.hentUregistrerteBarn()
                    ?: emptyList(),
            personerFremstiltKravFor =
                søknadGrunnlagService.finnPersonerFremstiltKravFor(
                    behandling = this,
                    forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(this),
                ),
        )

    @Transactional
    fun oppdaterEndringstidspunktOgGenererVedtaksperioderPåNytt(genererVedtaksperioderForOverstyrtEndringstidspunktDto: GenererVedtaksperioderForOverstyrtEndringstidspunktDto) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(genererVedtaksperioderForOverstyrtEndringstidspunktDto.behandlingId)

        validerBehandlingKanRedigeres(vedtak.behandling)

        lagreNedOverstyrtEndringstidspunkt(
            behandlingId = vedtak.behandling.id,
            overstyrtEndringstidspunkt = genererVedtaksperioderForOverstyrtEndringstidspunktDto.overstyrtEndringstidspunkt,
        )
        oppdaterVedtakMedVedtaksperioder(vedtak)
    }

    private fun lagreNedOverstyrtEndringstidspunkt(
        behandlingId: Long,
        overstyrtEndringstidspunkt: LocalDate,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        behandling.overstyrtEndringstidspunkt = overstyrtEndringstidspunkt
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling = behandling, sendTilDvh = false)
    }

    fun kopierOverVedtaksperioder(
        deaktivertVedtak: Vedtak,
        aktivtVedtak: Vedtak,
    ) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser =
                vedtaksperiodeHentOgPersisterService.lagre(
                    VedtaksperiodeMedBegrunnelser(
                        vedtak = aktivtVedtak,
                        fom = vedtaksperiodeMedBegrunnelser.fom,
                        tom = vedtaksperiodeMedBegrunnelser.tom,
                        type = vedtaksperiodeMedBegrunnelser.type,
                    ),
                )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                },
            )
            nyVedtaksperiodeMedBegrunnelser.settEØSBegrunnelser(
                vedtaksperiodeMedBegrunnelser.eøsBegrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                },
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                },
            )

            vedtaksperiodeHentOgPersisterService.lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> = vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)

    fun hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId: Long): List<UtvidetVedtaksperiodeMedBegrunnelserDto> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser().values.toList()
        val sanityEØSBegrunnelser = sanityService.hentSanityEØSBegrunnelser().values.toList()

        // For revurderinger med årsak klage skal fritekst støttes på alle begrunnelser
        val alleBegrunnelserSkalStøtteFritekst = behandling.type == BehandlingType.REVURDERING && behandling.erKlage()

        val vedtaksperioder =
            if (behandling.status != BehandlingStatus.AVSLUTTET) {
                val utvidetVedtaksperiodeMedBegrunnelser =
                    hentUtvidetVedtaksperiodeMedBegrunnelser(
                        vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId),
                        personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId),
                    )
                utvidetVedtaksperiodeMedBegrunnelser
                    .sorter()
                    .map {
                        it.tilUtvidetVedtaksperiodeMedBegrunnelserDto(
                            sanityBegrunnelser = sanityBegrunnelser,
                            sanityEØSBegrunnelser = sanityEØSBegrunnelser,
                            alleBegrunnelserSkalStøtteFritekst = alleBegrunnelserSkalStøtteFritekst,
                        )
                    }
            } else {
                emptyList()
            }

        val skalMinimeres = behandling.status != BehandlingStatus.UTREDES

        return if (skalMinimeres) {
            vedtaksperioder
                .filter { it.begrunnelser.isNotEmpty() }
                .map { it.copy(gyldigeBegrunnelser = emptyList()) }
        } else {
            vedtaksperioder
        }
    }

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(
        vedtak: Vedtak,
        personopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val persongrunnlag = personopplysningGrunnlag ?: persongrunnlagService.hentAktivThrows(vedtak.behandling.id)
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.map {
                it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    personopplysningGrunnlag = persongrunnlag,
                )
            }

        val skalSendeMedGyldigeBegrunnelser =
            behandling.status == BehandlingStatus.UTREDES && utvidetVedtaksperioderMedBegrunnelser.isNotEmpty()

        return if (skalSendeMedGyldigeBegrunnelser) {
            hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
                behandling = behandling,
                utvidedeVedtaksperioderMedBegrunnelser = utvidetVedtaksperioderMedBegrunnelser,
                vedtak = vedtak,
            )
        } else {
            utvidetVedtaksperioderMedBegrunnelser
        }
    }

    private fun hentUtvidetVedtaksperioderMedBegrunnelserOgGyldigeBegrunnelser(
        behandling: Behandling,
        utvidedeVedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak,
    ): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val grunnlagForBegrunnelser = hentGrunnlagForBegrunnelse(behandling)

        return utvidedeVedtaksperioderMedBegrunnelser.map { utvidetVedtaksperiodeMedBegrunnelser ->
            utvidetVedtaksperiodeMedBegrunnelser.copy(
                gyldigeBegrunnelser =
                    utvidetVedtaksperiodeMedBegrunnelser
                        .tilVedtaksperiodeMedBegrunnelser(vedtak)
                        .hentGyldigeBegrunnelserForPeriode(grunnlagForBegrunnelser)
                        .toList(),
            )
        }
    }

    fun hentGrunnlagForBegrunnelse(behandlingId: Long): GrunnlagForBegrunnelse = hentGrunnlagForBegrunnelse(behandlingHentOgPersisterService.hent(behandlingId))

    fun hentGrunnlagForBegrunnelse(behandling: Behandling): GrunnlagForBegrunnelse {
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        val behandlingsGrunnlagForVedtaksperioder = behandling.hentGrunnlagForVedtaksperioder()
        val behandlingsGrunnlagForVedtaksperioderForrigeBehandling = forrigeBehandling?.hentGrunnlagForVedtaksperioder()

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk = true)
        val sanityEØSBegrunnelser = sanityService.hentSanityEØSBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk = true)

        return GrunnlagForBegrunnelse(
            behandlingsGrunnlagForVedtaksperioder = behandlingsGrunnlagForVedtaksperioder,
            behandlingsGrunnlagForVedtaksperioderForrigeBehandling = behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
            sanityBegrunnelser = sanityBegrunnelser,
            sanityEØSBegrunnelser = sanityEØSBegrunnelser,
            nåDato = LocalDate.now(),
        )
    }

    fun oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
        vedtak: Vedtak,
        standardbegrunnelse: Standardbegrunnelse,
    ) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
            vedtaksperioder.singleOrNull()
                ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        fortsattInnvilgetPeriode.settBegrunnelser(
            listOf(
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                    standardbegrunnelse = standardbegrunnelse,
                ),
            ),
        )

        vedtaksperiodeHentOgPersisterService.lagre(fortsattInnvilgetPeriode)
    }

    fun hentUtbetalingsperioder(
        behandling: Behandling,
    ): List<Utbetalingsperiode> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
        return hentUtbetalingsperioder(behandling, personopplysningGrunnlag)
    }

    fun hentUtbetalingsperioder(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag?,
    ): List<Utbetalingsperiode> {
        if (personopplysningGrunnlag == null) return emptyList()

        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        return andelerTilkjentYtelse.mapTilUtbetalingsperioder(personopplysningGrunnlag = personopplysningGrunnlag)
    }

    fun skalHaÅrligKontroll(vedtak: Vedtak): Boolean =
        kompetanseRepository
            .finnFraBehandlingId(vedtak.behandling.id)
            .any { it.tom == null || it.tom.isAfter(YearMonth.now()) }

    fun skalMeldeFraOmEndringerEøsSelvstendigRett(vedtak: Vedtak): Boolean {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = vedtak.behandling.id)

        val annenForelderOmfattetAvNorskLovgivningErSattPåBosattIRiket = (
            vilkårsvurdering
                ?.personResultater
                ?.flatMap { it.vilkårResultater }
                ?.any { it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING) && it.vilkårType == Vilkår.BOSATT_I_RIKET }
                ?: false
        )

        val passendeBehandlingsresultat =
            vedtak.behandling.resultat !in
                listOf(
                    Behandlingsresultat.AVSLÅTT,
                    Behandlingsresultat.ENDRET_OG_OPPHØRT,
                    Behandlingsresultat.OPPHØRT,
                    Behandlingsresultat.ENDRET_OG_FORTSATT_OPPHØRT,
                )

        return annenForelderOmfattetAvNorskLovgivningErSattPåBosattIRiket && passendeBehandlingsresultat
    }

    fun beskrivPerioderMedFeilutbetaltValuta(vedtak: Vedtak): Set<String>? {
        val målform = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)?.søker?.målform
        val fra = mapOf(NB to "Fra", NN to "Frå").getOrDefault(målform, "Fra")
        val mye = mapOf(NB to "mye", NN to "mykje").getOrDefault(målform, "mye")

        return feilutbetaltValutaRepository
            .finnFeilutbetaltValutaForBehandling(vedtak.behandling.id)
            .map {
                if (it.erPerMåned) {
                    val måned = mapOf(NB to "måned", NN to "månad").getOrDefault(målform, "måned")
                    val (fom, tom) = it.fom.tilMånedÅr() to it.tom.tilMånedÅr()
                    "$fra $fom til $tom er det utbetalt ${it.feilutbetaltBeløp} kroner for $mye per $måned."
                } else {
                    val (fom, tom) = it.fom.tilDagMånedÅr() to it.tom.tilDagMånedÅr()
                    "$fra $fom til $tom er det utbetalt ${it.feilutbetaltBeløp} kroner for $mye."
                }
            }.toSet()
            .takeIf { it.isNotEmpty() }
    }

    fun beskrivPerioderMedRefusjonEøs(
        behandling: Behandling,
        avklart: Boolean,
    ): Set<String>? {
        val målform = persongrunnlagService.hentAktiv(behandlingId = behandling.id)?.søker?.målform
        val landkoderISO2 = kodeverkService.hentLandkoderISO2()

        return refusjonEøsRepository
            .finnRefusjonEøsForBehandling(behandling.id)
            .filter { it.refusjonAvklart == avklart }
            .map {
                val (fom, tom) = it.fom.tilMånedÅr() to it.tom.tilMånedÅr()
                val land =
                    landkoderISO2[it.land]?.storForbokstav() ?: throw Feil("Fant ikke navn for landkode ${it.land}")
                val beløp = it.refusjonsbeløp

                when (målform) {
                    NN -> {
                        if (avklart) {
                            "Frå $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Frå $fom til $tom blir ikkje etterbetaling på $beløp kroner per måned utbetalt no sidan det er utbetalt barnetrygd i $land."
                        }
                    }

                    else -> {
                        if (avklart) {
                            "Fra $fom til $tom blir etterbetaling på $beløp kroner per måned utbetalt til myndighetene i $land."
                        } else {
                            "Fra $fom til $tom blir ikke etterbetaling på $beløp kroner per måned utbetalt nå siden det er utbetalt barnetrygd i $land."
                        }
                    }
                }
            }.toSet()
            .takeIf { it.isNotEmpty() }
    }
}
