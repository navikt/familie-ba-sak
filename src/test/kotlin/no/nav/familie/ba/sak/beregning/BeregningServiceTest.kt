package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@Disabled
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
    private lateinit var behandlingService: BehandlingService

    @Test
    fun skalLagreRiktigBeregningsresultatForFGB() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGB(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                dagensDato.withDayOfMonth(1),
                dagensDato.plusMonths(10)
        )

        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(dagensDato.withDayOfMonth(1), beregningResultat.stønadFom)
        Assertions.assertEquals(dagensDato.plusMonths(10), beregningResultat.stønadTom)
        Assertions.assertNull(beregningResultat.opphørFom)

    }

    @Test
    fun skalLagreRiktigBeregningsresultatForOpphør() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val opphørsDato = dagensDato.plusMonths(1).withDayOfMonth(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForOpphør(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                dagensDato.withDayOfMonth(1),
                dagensDato.plusMonths(10),
                opphørsDato
        )

        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(dagensDato.plusMonths(10), beregningResultat.stønadTom)
        Assertions.assertNotNull(beregningResultat.opphørFom)
        Assertions.assertEquals(opphørsDato, beregningResultat.opphørFom)
    }

    @Test
    fun skalLagreRiktigBeregningsresultatForRevurdering() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val revurderingFom = dagensDato.plusMonths(3).withDayOfMonth(1)
        val opphørFom = dagensDato.withDayOfMonth(1)
        val tomDato = dagensDato.plusMonths(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
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
        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(revurderingFom, beregningResultat.stønadFom)
        Assertions.assertEquals(tomDato, beregningResultat.stønadTom)
        Assertions.assertEquals(opphørFom, beregningResultat.opphørFom)
    }
}