package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class EndringstidspunktService(
    private val behandlingRepository: BehandlingRepository,
    private val kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {
    fun finnEndringstidspunktForBehandling(behandlingId: Long): LocalDate {
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        // Hvis det ikke finnes en forrige behandling vil vi ha med alt (derfor setter vi endringstidspunkt til tidenes morgen)
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id) ?: return TIDENES_MORGEN

        val endringstidspunktUtbetalingsbeløp: YearMonth? = finnEndringstidspunktForBeløp(inneværendeBehandlingId = behandlingId, forrigeBehandlingId = forrigeBehandling.id)

        val endringstidspunktKompetanse: YearMonth? = finnEndringstidspunktForKompetanse()

        val endringstidspunktVilkårsvurdering: YearMonth? = finnEndringstidspunktForVilkårsvurdering()

        val endringstidspunktEndretUtbetalingAndeler: YearMonth? = finnEndringstidspunktForEndretUtbetalingAndel()

        return listOfNotNull(
            endringstidspunktUtbetalingsbeløp,
            endringstidspunktKompetanse,
            endringstidspunktVilkårsvurdering,
            endringstidspunktEndretUtbetalingAndeler
        ).minOfOrNull { it }?.førsteDagIInneværendeMåned() ?: TIDENES_MORGEN
    }

    private fun finnEndringstidspunktForBeløp(inneværendeBehandlingId: Long, forrigeBehandlingId: Long): YearMonth? {
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandlingId)

        return EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler
        )
    }
    private fun finnEndringstidspunktForKompetanse(): YearMonth? = TODO()
    private fun finnEndringstidspunktForVilkårsvurdering(): YearMonth? = TODO()
    private fun finnEndringstidspunktForEndretUtbetalingAndel(): YearMonth? = TODO()

    @Deprecated("Skal erstattes av finnEndringstidpunkForBehandling")
    fun finnEndringstidpunkForBehandlingGammel(behandlingId: Long): LocalDate {
        val nyBehandling = behandlingRepository.finnBehandling(behandlingId)

        val alleAvsluttetBehandlingerPåFagsak =
            behandlingRepository.findByFagsakAndAvsluttet(fagsakId = nyBehandling.fagsak.id)
        val sisteVedtattBehandling = Behandlingutils.hentSisteBehandlingSomErVedtatt(alleAvsluttetBehandlingerPåFagsak)
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
}
