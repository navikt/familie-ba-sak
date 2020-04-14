package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
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
@ActiveProfiles("postgres")
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
    private lateinit var behandlingResultatService: BehandlingResultatService

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

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

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

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

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

        val tilkjentYtelse = beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertEquals(revurderingFom, tilkjentYtelse.stønadFom)
        Assertions.assertEquals(tomDato, tilkjentYtelse.stønadTom)
        Assertions.assertEquals(opphørFom, tilkjentYtelse.opphørFom)
    }

    @Test
    fun `Skal Lagre AndelerTilkjentYtelse Med Kobling Til TilkjentYtelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val dato_2020_01_01 = LocalDate.of(2020, 1, 1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val barn1Id = personopplysningGrunnlag.barna.find { it.personIdent.ident == barn1Fnr }!!.id
        val barn2Id = personopplysningGrunnlag.barna.find { it.personIdent.ident == barn2Fnr }!!.id

        val behandlingResultat = BehandlingResultat(behandling = behandling)
        behandlingResultat.periodeResultater = lagPeriodeResultaterForSøkerOgToBarn(søkerFnr, barn1Fnr, barn2Fnr, dato_2020_01_01, dato_2020_01_01.plusYears(17))//TODO: sjekk om personresultat bør lages først
        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val andelBarn1 = tilkjentYtelse.andelerTilkjentYtelse.find { it.personId == barn1Id }
        val andelBarn2 = tilkjentYtelse.andelerTilkjentYtelse.find { it.personId == barn2Id }

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
        Assertions.assertNotNull(andelBarn1)
        Assertions.assertNotNull(andelBarn2)
        tilkjentYtelse.andelerTilkjentYtelse.forEach {
            Assertions.assertEquals(tilkjentYtelse, it.tilkjentYtelse)
        }
        Assertions.assertEquals(1054, andelBarn1!!.beløp)
        Assertions.assertEquals(1054, andelBarn2!!.beløp)
    }

    private fun opprettTilkjentYtelse(behandling: Behandling) {
        tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling))
    }
}