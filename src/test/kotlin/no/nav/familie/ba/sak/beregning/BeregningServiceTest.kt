package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        val utbetalingsoppdrag = Utbetalingsoppdrag(
                Utbetalingsoppdrag.KodeEndring.NY,
                "BA",
                fagsak.id.toString(),
                UUID.randomUUID().toString(),
                "SAKSBEHANDLERID",
                LocalDateTime.now(),
                listOf(Utbetalingsperiode(false,
                        null,
                        1,
                        null,
                        dagensDato,
                        "BATR",
                        dagensDato.withDayOfMonth(1),
                        dagensDato.plusMonths(10),
                        BigDecimal.ONE,
                        Utbetalingsperiode.SatsType.MND,
                        fnr,
                        behandling.id
                ))
        )

        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(dagensDato.withDayOfMonth(1), beregningResultat.stønadFom)
        Assertions.assertEquals(dagensDato.plusMonths(10), beregningResultat.stønadTom)
        Assertions.assertFalse(beregningResultat.erOpphør)

    }

    @Test
    fun skalLagreRiktigBeregningsresultatForOpphoer() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val opphørsDato = dagensDato.plusMonths(1).withDayOfMonth(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val utbetalingsoppdrag = Utbetalingsoppdrag(
                Utbetalingsoppdrag.KodeEndring.NY,
                "BA",
                fagsak.id.toString(),
                UUID.randomUUID().toString(),
                "SAKSBEHANDLERID",
                LocalDateTime.now(),
                listOf(Utbetalingsperiode(true,
                        Opphør(opphørsDato),
                        1,
                        null,
                        dagensDato,
                        "BATR",
                        dagensDato.withDayOfMonth(1),
                        dagensDato.plusMonths(10),
                        BigDecimal.ONE,
                        Utbetalingsperiode.SatsType.MND,
                        fnr,
                        behandling.id
                ))
        )

        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(opphørsDato, beregningResultat.stønadFom)
        Assertions.assertEquals(dagensDato.plusMonths(10), beregningResultat.stønadTom)
        Assertions.assertTrue(beregningResultat.erOpphør)
    }

    @Test
    fun skalLagreRiktigBeregningsresultatForRevurdering() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val revurderingFra = dagensDato.plusMonths(3).withDayOfMonth(1)
        val tomDato = dagensDato.plusMonths(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val utbetalingsoppdrag = Utbetalingsoppdrag(
                Utbetalingsoppdrag.KodeEndring.NY,
                "BA",
                fagsak.id.toString(),
                UUID.randomUUID().toString(),
                "SAKSBEHANDLERID",
                LocalDateTime.now(),
                listOf(Utbetalingsperiode(true,
                        Opphør(dagensDato.withDayOfMonth(1)),
                        1,
                        null,
                        dagensDato,
                        "BATR",
                        dagensDato.withDayOfMonth(1),
                        tomDato,
                        BigDecimal.ONE,
                        Utbetalingsperiode.SatsType.MND,
                        fnr,
                        behandling.id
                ), Utbetalingsperiode(false,
                        null,
                        2,
                        1,
                        dagensDato,
                        "BATR",
                        revurderingFra,
                        tomDato,
                        BigDecimal.ONE,
                        Utbetalingsperiode.SatsType.MND,
                        fnr,
                        behandling.id
                ))
        )

        beregningService.lagreBeregningsresultat(behandling, utbetalingsoppdrag)
        val beregningResultat = beregningService.hentBeregningsresultatForBehandling(behandling.id)

        Assertions.assertNotNull(beregningResultat)
        Assertions.assertEquals(revurderingFra, beregningResultat.stønadFom)
        Assertions.assertEquals(tomDato, beregningResultat.stønadTom)
        Assertions.assertFalse(beregningResultat.erOpphør)
    }
}