package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
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
@ActiveProfiles("postgres", "mock-pdl", "mock-arbeidsfordeling", "mock-infotrygd-barnetrygd")
@Tag("integration")
class BeregningServiceIntegrationTest {

    @Autowired
    private lateinit var beregningService: BeregningService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Test
    fun skalLagreRiktigTilkjentYtelseForFGBMedToBarn() {
        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val fomBarn1 = dagensDato.withDayOfMonth(1)
        val fomBarn2 = fomBarn1.plusYears(2)
        val tomBarn1 = fomBarn1.plusYears(18).sisteDagIMåned()
        val tomBarn2 = fomBarn2.plusYears(18).sisteDagIMåned()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGBMedToBarn(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                fomBarn1,
                tomBarn1,
                fomBarn2,
                tomBarn2
        )

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(fomBarn1.toYearMonth(), tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomBarn2.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertNull(tilkjentYtelse.opphørFom)

    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForOpphørMedToBarn() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val fomBarn1 = dagensDato.withDayOfMonth(1)
        val fomBarn2 = fomBarn1.plusYears(2)
        val tomBarn1 = fomBarn1.plusYears(18).sisteDagIMåned()
        val tomBarn2 = fomBarn2.plusYears(18).sisteDagIMåned()
        val opphørsDato = fomBarn1.plusYears(5).withDayOfMonth(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForOpphørMedToBarn(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                dagensDato,
                fomBarn1,
                tomBarn1,
                fomBarn2,
                tomBarn2,
                opphørsDato
        )

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertNull(tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomBarn2.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertNotNull(tilkjentYtelse.opphørFom)
        Assertions.assertEquals(opphørsDato.toYearMonth(), tilkjentYtelse.opphørFom)
    }

    @Test
    fun skalLagreRiktigTilkjentYtelseForRevurderingMedToBarn() {

        val fnr = randomFnr()
        val dagensDato = LocalDate.now()
        val opphørFomBarn1 = LocalDate.of(2020, 5, 1)
        val revurderingFomBarn1 = LocalDate.of(2020, 7, 1)
        val fomDatoBarn1 = LocalDate.of(2020, 1, 1)
        val tomDatoBarn1 = fomDatoBarn1.plusYears(18).sisteDagIForrigeMåned()

        val opphørFomBarn2 = LocalDate.of(2020, 8, 1)
        val revurderingFomBarn2 = LocalDate.of(2020, 10, 1)
        val fomDatoBarn2 = LocalDate.of(2019, 10, 1)
        val tomDatoBarn2 = fomDatoBarn2.plusYears(18).sisteDagIForrigeMåned()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING))
        opprettTilkjentYtelse(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForRevurderingMedToBarn(
                fnr,
                fagsak.id.toString(),
                behandling.id,
                behandling.id - 1,
                dagensDato,
                opphørFomBarn1,
                revurderingFomBarn1,
                fomDatoBarn1,
                tomDatoBarn1,
                opphørFomBarn2,
                revurderingFomBarn2,
                fomDatoBarn2,
                tomDatoBarn2
        )

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(revurderingFomBarn1.toYearMonth(), tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomDatoBarn1.toYearMonth(), tilkjentYtelse.stønadTom)
        Assertions.assertEquals(opphørFomBarn2.toYearMonth(), tilkjentYtelse.opphørFom)
    }

    @Test
    fun `Skal lagre andelerTilkjentYtelse med kobling til TilkjentYtelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val dato_2021_11_01 = LocalDate.of(2021, 11, 1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val barn1Id = personopplysningGrunnlag.barna.find { it.personIdent.ident == barn1Fnr }!!.personIdent.ident
        val barn2Id = personopplysningGrunnlag.barna.find { it.personIdent.ident == barn2Fnr }!!.personIdent.ident

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = lagPersonResultaterForSøkerOgToBarn(vilkårsvurdering,
                                                                                søkerFnr,
                                                                                barn1Fnr,
                                                                                barn2Fnr,
                                                                                dato_2021_11_01,
                                                                                dato_2021_11_01.plusYears(17))
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)!!
        val andelBarn1 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.personIdent == barn1Id }
        val andelBarn2 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.personIdent == barn2Id }

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
        Assertions.assertEquals(2, andelBarn1.size)
        Assertions.assertEquals(2, andelBarn2.size)
        tilkjentYtelse.andelerTilkjentYtelse.forEach {
            Assertions.assertEquals(tilkjentYtelse, it.tilkjentYtelse)
        }
        Assertions.assertEquals(1, andelBarn1.filter { it.beløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn1.filter { it.beløp == 1654 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.beløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.beløp == 1654 }.size)
    }

    private fun opprettTilkjentYtelse(behandling: Behandling) {
        tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling))
    }
}