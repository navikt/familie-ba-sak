package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class BeregningServiceTest {

    @Autowired
    private lateinit var beregningService: BeregningService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Test
    fun skalLagreRiktigTilkjentYtelseForFGB() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGB(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                dagensDato.withDayOfMonth(1),
                dagensDato.plusMonths(10)
        )

        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandling.id)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(dagensDato.withDayOfMonth(1), tilkjentYtelse.stønadFom)
        Assertions.assertEquals(dagensDato.plusMonths(10), tilkjentYtelse.stønadTom)
        Assertions.assertNull(tilkjentYtelse.opphørFom)

    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForOpphør() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val opphørsDato = dagensDato.plusMonths(1).withDayOfMonth(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForOpphør(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                dagensDato.withDayOfMonth(1),
                dagensDato.plusMonths(10),
                opphørsDato
        )


        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandling.id)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(dagensDato.plusMonths(10), tilkjentYtelse.stønadTom)
        Assertions.assertNotNull(tilkjentYtelse.opphørFom)
        Assertions.assertEquals(opphørsDato, tilkjentYtelse.opphørFom)
    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForRevurdering() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val revurderingFom = dagensDato.plusMonths(3).withDayOfMonth(1)
        val opphørFom = dagensDato.withDayOfMonth(1)
        val tomDato = dagensDato.plusMonths(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForRevurdering(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                behandling.id - 1,
                dagensDato,
                opphørFom,
                tomDato,
                revurderingFom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandling.id)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(revurderingFom, tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomDato, tilkjentYtelse.stønadTom)
        Assertions.assertEquals(opphørFom, tilkjentYtelse.opphørFom)
    }

    private fun opprettTilkjentYtelse(behandling: Behandling) {
        tilkjentYtelseRepository.save(TilkjentYtelse(
                behandling = behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                andelerTilkjentYtelse = emptySet()
        ))
    }
}