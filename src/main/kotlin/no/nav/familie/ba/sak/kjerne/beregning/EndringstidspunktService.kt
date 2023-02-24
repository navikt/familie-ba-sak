package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIEndretUtbetalingAndelUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIVilkårsvurderingUtil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class EndringstidspunktService(
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {
    fun finnEndringstidspunktForBehandling(behandlingId: Long): LocalDate {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        // Hvis det ikke finnes en forrige behandling vil vi ha med alt (derfor setter vi endringstidspunkt til tidenes morgen)
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id) ?: return TIDENES_MORGEN

        val endringstidspunktUtbetalingsbeløp = finnEndringstidspunktForBeløp(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktKompetanse = finnEndringstidspunktForKompetanse(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktVilkårsvurdering = finnEndringstidspunktForVilkårsvurdering(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktEndretUtbetalingAndeler = finnEndringstidspunktForEndretUtbetalingAndel(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val tidligsteEndringstidspunkt = utledEndringstidspunkt(
            endringstidspunktUtbetalingsbeløp = endringstidspunktUtbetalingsbeløp,
            endringstidspunktKompetanse = endringstidspunktKompetanse,
            endringstidspunktVilkårsvurdering = endringstidspunktVilkårsvurdering,
            endringstidspunktEndretUtbetalingAndeler = endringstidspunktEndretUtbetalingAndeler
        )

        logger.info(
            "Tidligste endringstidspunkt ble utledet til å være $tidligsteEndringstidspunkt." +
                "Endringstidspunkt utbetalingsbeløp: $endringstidspunktUtbetalingsbeløp" +
                "Endringstidspunkt kompetanse: $endringstidspunktKompetanse" +
                "Endringstidspunkt vilkårsvurdering: $endringstidspunktVilkårsvurdering" +
                "Endringstidspunkt endret utbetaling andeler: $endringstidspunktEndretUtbetalingAndeler"
        )

        return tidligsteEndringstidspunkt
    }

    private fun finnEndringstidspunktForBeløp(inneværendeBehandlingId: Long, forrigeBehandlingId: Long): YearMonth? {
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandlingId)

        return EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler
        )
    }
    private fun finnEndringstidspunktForKompetanse(inneværendeBehandlingId: Long, forrigeBehandlingId: Long): YearMonth? {
        val nåværendeKompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId = inneværendeBehandlingId).toList()
        val forrigeKompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId = forrigeBehandlingId).toList()

        return EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser
        )
    }
    private fun finnEndringstidspunktForVilkårsvurdering(inneværendeBehandlingId: Long, forrigeBehandlingId: Long): YearMonth? {
        val nåværendeVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = inneværendeBehandlingId) ?: return null
        val forrigeVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandlingId) ?: return null

        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandlingId)
        val nåværendeEndretAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = inneværendeBehandlingId)

        val opphørstidspunkt = nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(forrigeAndeler, nåværendeEndretAndeler) ?: return null

        return EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = nåværendeVilkårsvurdering.personResultater,
            forrigePersonResultat = forrigeVilkårsvurdering.personResultater,
            opphørstidspunkt = opphørstidspunkt
        )
    }
    private fun finnEndringstidspunktForEndretUtbetalingAndel(inneværendeBehandlingId: Long, forrigeBehandlingId: Long): YearMonth? {
        val nåværendeEndretAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeEndretAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = forrigeBehandlingId)

        return EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler
        )
    }

    @Deprecated("Skal erstattes av finnEndringstidpunkForBehandling")
    fun finnEndringstidpunkForBehandlingGammel(behandlingId: Long): LocalDate {
        val nyBehandling = behandlingHentOgPersisterService.hent(behandlingId)

        val sisteVedtattBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = nyBehandling.fagsak.id)
            ?: return TIDENES_MORGEN

        val nyeAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val forrigeAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteVedtattBehandling.id)

        val førsteEndringstidspunktFraAndelTilkjentYtelse = nyeAndelerTilkjentYtelse.hentFørsteEndringstidspunkt(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse
        ) ?: TIDENES_ENDE

        val kompetansePerioder = kompetanseRepository.finnFraBehandlingId(nyBehandling.id)
        val forrigeKompetansePerioder = kompetanseRepository.finnFraBehandlingId(sisteVedtattBehandling.id)
        val førsteEndringstidspunkt = kompetansePerioder.finnFørsteEndringstidspunkt(forrigeKompetansePerioder)

        val førsteEndringstidspunktIKompetansePerioder =
            if (førsteEndringstidspunkt != TIDENES_ENDE.toYearMonth()) {
                førsteEndringstidspunkt.førsteDagIInneværendeMåned()
            } else {
                TIDENES_ENDE
            }

        return minOf(førsteEndringstidspunktFraAndelTilkjentYtelse, førsteEndringstidspunktIKompetansePerioder)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EndringstidspunktService::class.java)
    }
}
