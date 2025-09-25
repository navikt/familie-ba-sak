package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidering
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer
import no.nav.familie.ba.sak.kjerne.endretutbetaling.validerBarnasVilkår
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.småbarnstillegg.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlingSteg
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.lagForskjøvetTidslinjeForOppfylteVilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

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
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val valutakursRepository: ValutakursRepository,
    private val clockProvider: ClockProvider,
    private val kompetanseRepository: KompetanseRepository,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) : BehandlingSteg<String> {
    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        if ((!behandling.erSatsendringEllerMånedligValutajustering() || !behandling.erFinnmarksTilleggEllerSvalbardtillegg()) && behandling.skalBehandlesAutomatisk) {
            return
        }

        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandling.id)
        if (behandling.type != BehandlingType.TEKNISK_ENDRING && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandlingId = behandling.id)

            validerBarnasVilkår(søkerOgBarn.barn(), vilkårsvurdering)
        }

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val andelerForrigeBehandling by lazy { beregningService.hentAndelerFraForrigeVedtatteBehandling(behandling) }

        if (behandling.erSatsendring()) {
            validerSatsendring(tilkjentYtelse)
        }

        if (behandling.erFinnmarkstillegg()) {
            validerFinnmarkstilleggBehandling(tilkjentYtelse)
        }

        if (behandling.erSvalbardtillegg()) {
            validerSvalbardtilleggBehandling(tilkjentYtelse)
        }

        validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
            tilkjentYtelse = tilkjentYtelse,
            søkerOgBarn = søkerOgBarn,
        )

        if (!behandling.erSatsendringEllerMånedligValutajustering() || !behandling.erFinnmarksTilleggEllerSvalbardtillegg()) {
            val endreteUtbetalingerMedAndeler =
                andelerTilkjentYtelseOgEndreteUtbetalingerService
                    .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
            endreteUtbetalingerMedAndeler.validerEndredeUtbetalingsandeler(tilkjentYtelse, vilkårService.hentVilkårsvurdering(behandling.id))

            validerKompetanse(behandling.id)
        }

        if (behandling.erMånedligValutajustering()) {
            BehandlingsresultatValideringUtils.validerIngenEndringTilbakeITid(
                andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse,
                andelerForrigeBehandling = andelerForrigeBehandling,
                nåMåned = YearMonth.now(clockProvider.get()),
            )
            BehandlingsresultatValideringUtils.validerSatsErUendret(
                andelerDenneBehandlingen = tilkjentYtelse.andelerTilkjentYtelse,
                andelerForrigeBehandling = andelerForrigeBehandling,
            )
        }

        if (behandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO) {
            validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling)
        }
    }

    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        val behandlingMedOppdatertBehandlingsresultat =
            if (behandling.erMigrering() && behandling.skalBehandlesAutomatisk) {
                settBehandlingsresultat(behandling, Behandlingsresultat.INNVILGET)
            } else {
                val resultat = behandlingsresultatService.utledBehandlingsresultat(behandlingId = behandling.id)

                behandlingService.oppdaterBehandlingsresultat(
                    behandlingId = behandling.id,
                    resultat = resultat,
                )
            }

        validerBehandlingsresultatErGyldigForÅrsak(behandlingMedOppdatertBehandlingsresultat)

        validerAtUtenlandskPeriodeBeløpOgValutakursErUtfylt(behandling = behandling)

        if (behandlingMedOppdatertBehandlingsresultat.erBehandlingMedVedtaksbrevutsending()) {
            behandlingService.nullstillEndringstidspunkt(behandling.id)
            vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(
                vedtak =
                    vedtakService.hentAktivForBehandlingThrows(
                        behandlingId = behandling.id,
                    ),
            )
        }

        val endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi =
            beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling)

        val skalRettFraBehandlingsresultatTilIverksetting =
            behandlingMedOppdatertBehandlingsresultat.skalRettFraBehandlingsresultatTilIverksetting(
                endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi == ENDRING_I_UTBETALING,
            )

        val sistIverksatteBehandling by lazy { behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandlingMedOppdatertBehandlingsresultat) }

        if (skalRettFraBehandlingsresultatTilIverksetting ||
            småbarnstilleggService.kanAutomatiskIverksetteSmåbarnstilleggEndring(
                behandling = behandlingMedOppdatertBehandlingsresultat,
                sistIverksatteBehandling = sistIverksatteBehandling,
            )
        ) {
            behandlingService.oppdaterStatusPåBehandling(
                behandlingMedOppdatertBehandlingsresultat.id,
                BehandlingStatus.IVERKSETTER_VEDTAK,
            )
        } else {
            simuleringService.oppdaterSimuleringPåBehandling(behandlingMedOppdatertBehandlingsresultat)
        }

        tilbakestillBehandlingService.slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(behandling.id)

        return hentNesteStegGittEndringerIUtbetaling(
            behandling,
            endringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi,
        )
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (behandling.opprettetÅrsak.erOmregningsårsak() &&
            behandling.resultat !in listOf(FORTSATT_INNVILGET, FORTSATT_OPPHØRT)
        ) {
            throw Feil("Behandling $behandling er omregningssak men er ikke uendret behandlingsresultat")
        }
    }

    override fun stegType(): StegType = StegType.BEHANDLINGSRESULTAT

    private fun validerBehandlingsresultatErGyldigForÅrsak(behandlingMedOppdatertBehandlingsresultat: Behandling) {
        if (behandlingMedOppdatertBehandlingsresultat.erManuellMigrering() &&
            (
                behandlingMedOppdatertBehandlingsresultat.resultat.erAvslått() ||
                    behandlingMedOppdatertBehandlingsresultat.resultat == Behandlingsresultat.DELVIS_INNVILGET
            )
        ) {
            throw FunksjonellFeil(
                "Du har fått behandlingsresultatet " +
                    "${behandlingMedOppdatertBehandlingsresultat.resultat.displayName}. " +
                    "Dette er ikke støttet på migreringsbehandlinger. " +
                    "Meld sak i Porten om du er uenig i resultatet.",
            )
        }
    }

    private fun settBehandlingsresultat(
        behandling: Behandling,
        resultat: Behandlingsresultat,
    ): Behandling {
        behandling.resultat = resultat
        return behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    private fun validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(behandling: Behandling) {
        if (behandling.status == BehandlingStatus.AVSLUTTET) return

        val endringIUtbetalingTidslinje =
            beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling)

        val migreringsdatoForrigeIverksatteBehandling =
            beregningService
                .hentAndelerFraForrigeIverksattebehandling(behandling)
                .minOfOrNull { it.stønadFom }

        endringIUtbetalingTidslinje.kastFeilVedEndringEtter(
            migreringsdatoForrigeIverksatteBehandling =
                migreringsdatoForrigeIverksatteBehandling
                    ?: TIDENES_ENDE.toYearMonth(),
            behandling = behandling,
        )
    }

    private fun validerSatsendring(tilkjentYtelse: TilkjentYtelse) {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(tilkjentYtelse.behandling)
                ?: throw FunksjonellFeil("Kan ikke kjøre satsendring når det ikke finnes en tidligere behandling på fagsaken")
        val andelerFraForrigeBehandling =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
            andelerFraForrigeBehandling = andelerFraForrigeBehandling,
            andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
        )
    }

    private fun validerFinnmarkstilleggBehandling(tilkjentYtelse: TilkjentYtelse) {
        val behandling = tilkjentYtelse.behandling
        val vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandlingId = behandling.id)
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(tilkjentYtelse.behandling)
                ?: throw Feil("Kan ikke kjøre finnmarkstillegg behandling dersom det ikke finnes en tidligere iverksatt behandling")

        val andelerNåværendeBehandling = tilkjentYtelse.andelerTilkjentYtelse.toList()
        val andelerFraForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerFraForrigeBehandling,
            ytelseType = YtelseType.FINNMARKSTILLEGG,
            behandlingÅrsak = BehandlingÅrsak.FINNMARKSTILLEGG,
        )

        validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
            andeler = andelerNåværendeBehandling,
            ytelseType = YtelseType.FINNMARKSTILLEGG,
        )

        validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark(vilkårsvurdering)
    }

    private fun validerSvalbardtilleggBehandling(tilkjentYtelse: TilkjentYtelse) {
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(tilkjentYtelse.behandling)
                ?: throw Feil("Kan ikke kjøre svalbardtillegg behandling dersom det ikke finnes en tidligere iverksatt behandling")

        val andelerNåværendeBehandling = tilkjentYtelse.andelerTilkjentYtelse.toList()
        val andelerFraForrigeBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id)

        validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
            andelerNåværendeBehandling = andelerNåværendeBehandling,
            andelerForrigeBehandling = andelerFraForrigeBehandling,
            ytelseType = YtelseType.SVALBARDTILLEGG,
            behandlingÅrsak = BehandlingÅrsak.SVALBARDTILLEGG,
        )

        validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
            andeler = andelerNåværendeBehandling,
            ytelseType = YtelseType.SVALBARDTILLEGG,
        )
    }

    private fun validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark(vilkårsvurdering: Vilkårsvurdering) {
        val søkersPersonResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() } ?: return

        val søkerBosattIFinnmarkTidslinje =
            søkersPersonResultat.vilkårResultater
                .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_I_FINNMARK_NORD_TROMS in it.utdypendeVilkårsvurderinger }
                .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

        vilkårsvurdering
            .personResultater
            .filterNot { it.erSøkersResultater() }
            .forEach { personResultat ->
                val barnBosattIFinnmarkTidslinje =
                    personResultat.vilkårResultater
                        .filter { it.vilkårType == BOSATT_I_RIKET && BOSATT_I_FINNMARK_NORD_TROMS in it.utdypendeVilkårsvurderinger }
                        .lagForskjøvetTidslinjeForOppfylteVilkår(BOSATT_I_RIKET)

                val barnDeltBostedTidslinje =
                    personResultat.vilkårResultater
                        .filter { it.vilkårType == BOR_MED_SØKER && (it.utdypendeVilkårsvurderinger.contains(DELT_BOSTED) || it.utdypendeVilkårsvurderinger.contains(DELT_BOSTED_SKAL_IKKE_DELES)) }
                        .lagForskjøvetTidslinjeForOppfylteVilkår(BOR_MED_SØKER)

                val finnesPerioderDerBarnMedDeltBostedIkkeBorSammenMedSøkerIFinnmark =
                    søkerBosattIFinnmarkTidslinje.kombinerMed(barnBosattIFinnmarkTidslinje, barnDeltBostedTidslinje) { søkerBosattIFinnmark, barnBosattIFinnmark, barnDeltBosted ->
                        søkerBosattIFinnmark != null && barnBosattIFinnmark == null && barnDeltBosted != null
                    }

                if (finnesPerioderDerBarnMedDeltBostedIkkeBorSammenMedSøkerIFinnmark.tilPerioder().any { it.verdi == true }) {
                    throw Feil("Det finnes perioder der søker bor i finnmark samtidig som et barn med delt bosted ikke bor i finnmark. Disse sakene støtter vi ikke automatisk, og vi stanser derfor denne behandlingen.")
                }
            }
    }

    private fun validerAtDetIkkeHarVærtEndringIUtbetalingUtenomYtelseType(
        andelerNåværendeBehandling: List<AndelTilkjentYtelse>,
        andelerForrigeBehandling: List<AndelTilkjentYtelse>,
        ytelseType: YtelseType,
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val andelerUtenomYtelseTypeDenneBehandling = andelerNåværendeBehandling.filterNot { it.type == ytelseType }
        val andelerUtenomYtelseTypeForrigeBehandling = andelerForrigeBehandling.filterNot { it.type == ytelseType }

        val erEndringIUtbetaling =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = andelerUtenomYtelseTypeDenneBehandling,
                    forrigeAndeler = andelerUtenomYtelseTypeForrigeBehandling,
                ).tilPerioder()
                .any { it.verdi == true }

        if (erEndringIUtbetaling) {
            throw Feil("Det er oppdaget forskjell i utbetaling utenom $ytelseType andeler. Dette kan ikke skje i en behandling der årsak er $behandlingÅrsak, og den automatiske kjøring stoppes derfor.")
        }
    }

    private fun validerAtIngenAndelerMedYtelseTypeErInnvilgetFramITid(
        andeler: List<AndelTilkjentYtelse>,
        ytelseType: YtelseType,
    ) {
        val andelerMedYtelseType = andeler.filter { it.type == ytelseType }
        val dagensDato = YearMonth.now(clockProvider.get())
        val enMånedFramITid = dagensDato.plusMonths(1)

        andelerMedYtelseType
            .groupBy { it.aktør }
            .forEach { (_, andel) ->
                val tidligsteAndelMedYtelseTypeForAktør = andel.minOfOrNull { it.stønadFom } ?: return@forEach

                // TODO: Fiks valideringen når vi går live i oktober
                if ((tidligsteAndelMedYtelseTypeForAktør > enMånedFramITid) && dagensDato >= YearMonth.of(2025, 10)) {
                    throw Feil("Det eksisterer $ytelseType andeler som først blir innvilget mer enn 1 måned fram i tid. Det er ikke mulig å innvilge disse enda, og behandlingen stoppes derfor.")
                }
            }
    }

    private fun validerAtUtenlandskPeriodeBeløpOgValutakursErUtfylt(behandling: Behandling) {
        val utenlandskePeriodeBeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId = behandling.id)
        val valutakurser by lazy { valutakursRepository.finnFraBehandlingId(behandlingId = behandling.id) }

        if (utenlandskePeriodeBeløp.any { !it.erObligatoriskeFelterSatt() } || valutakurser.any { !it.erObligatoriskeFelterSatt() }) {
            throw FunksjonellFeil("Kan ikke fullføre behandlingsresultat-steg før utbetalt i det andre landet og valutakurs er fylt ut for alle barn og perioder")
        }
    }

    private fun validerKompetanse(behandlingId: Long) {
        val kompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId)

        validerAtAktivitetslandOgBostedIkkeErNorgeHvisNorgeErSekundærland(kompetanser)
    }

    private fun validerAtAktivitetslandOgBostedIkkeErNorgeHvisNorgeErSekundærland(kompetanser: Collection<Kompetanse>) {
        kompetanser.forEach { kompetanse ->
            val erNorgeSekundærland = kompetanse.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND

            if (!erNorgeSekundærland) return@forEach

            if (setOf(kompetanse.søkersAktivitetsland, kompetanse.annenForeldersAktivitetsland, kompetanse.barnetsBostedsland)
                    .all { it == "NO" }
            ) {
                throw FunksjonellFeil("Dersom Norge er sekundærland, må søkers aktivitetsland, annen forelders aktivitetsland eller barnets bostedsland være satt til noe annet enn Norge")
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)!!
    }
}

private fun List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>.validerEndredeUtbetalingsandeler(
    tilkjentYtelse: TilkjentYtelse,
    vilkårsvurdering: Vilkårsvurdering?,
) {
    EndretUtbetalingAndelValidering.validerAtAlleOpprettedeEndringerErUtfylt(map { it.endretUtbetalingAndel })
    EndretUtbetalingAndelValidering.validerAtEndringerErTilknyttetAndelTilkjentYtelse(this)
    validerAtDetFinnesDeltBostedEndringerMedSammeProsentForUtvidedeEndringer(
        endretUtbetalingAndelerMedÅrsakDeltBosted = filter { it.årsak == Årsak.DELT_BOSTED },
    )

    EndretUtbetalingAndelValidering.validerPeriodeInnenforTilkjentytelse(
        map { it.endretUtbetalingAndel },
        tilkjentYtelse.andelerTilkjentYtelse,
    )

    EndretUtbetalingAndelValidering.validerÅrsak(
        map { it.endretUtbetalingAndel },
        vilkårsvurdering,
    )
}
