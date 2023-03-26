package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
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
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val vilkårsvurderingService: VilkårsvurderingService
) {
    fun finnEndringstidspunktForBehandling(behandlingId: BehandlingId): LocalDate {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        // Hvis det ikke finnes en forrige behandling vil vi ha med alt (derfor setter vi endringstidspunkt til tidenes morgen)
        val forrigeBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)
                ?: return TIDENES_MORGEN

        val endringstidspunktUtbetalingsbeløp = finnEndringstidspunktForBeløp(
            inneværendeBehandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandling.behandlingId
        )

        val endringstidspunktKompetanse = finnEndringstidspunktForKompetanse(
            inneværendeBehandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandling.behandlingId
        )

        val endringstidspunktVilkårsvurdering = finnEndringstidspunktForVilkårsvurdering(
            inneværendeBehandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandling.behandlingId
        )

        val endringstidspunktEndretUtbetalingAndeler = finnEndringstidspunktForEndretUtbetalingAndel(
            inneværendeBehandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandling.behandlingId
        )

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

    private fun finnEndringstidspunktForBeløp(
        inneværendeBehandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId
    ): YearMonth? {
        val nåværendeAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = inneværendeBehandlingId.id)
        val forrigeAndeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeBehandlingId.id)

        return EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler
        )
    }

    private fun finnEndringstidspunktForKompetanse(
        inneværendeBehandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId
    ): YearMonth? {
        val nåværendeKompetanser =
            kompetanseRepository.finnFraBehandlingId(behandlingId = inneværendeBehandlingId.id).toList()
        val forrigeKompetanser =
            kompetanseRepository.finnFraBehandlingId(behandlingId = forrigeBehandlingId.id).toList()

        return EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser
        )
    }

    private fun finnEndringstidspunktForVilkårsvurdering(
        inneværendeBehandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId
    ): YearMonth? {
        val nåværendeVilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = inneværendeBehandlingId) ?: return null
        val forrigeVilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandlingId) ?: return null

        return EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = nåværendeVilkårsvurdering.personResultater,
            forrigePersonResultat = forrigeVilkårsvurdering.personResultater
        )
    }

    private fun finnEndringstidspunktForEndretUtbetalingAndel(
        inneværendeBehandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId
    ): YearMonth? {
        val nåværendeEndretAndeler =
            endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = inneværendeBehandlingId)
        val forrigeEndretAndeler =
            endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = forrigeBehandlingId)

        return EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EndringstidspunktService::class.java)
    }
}
