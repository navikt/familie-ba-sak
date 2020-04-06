package no.nav.familie.ba.sak.beregning

import io.mockk.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.toRestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.beregning.domene.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningServiceTest {

    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    val behandlingResultatRepository = mockk<BehandlingResultatRepository>()

    lateinit var satsService: SatsService
    lateinit var beregningService: BeregningService

    @BeforeEach
    fun setUp() {
        val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
        val fagsakService = mockk<FagsakService>()
        val satsRepository = mockk<SatsRepository>()

        satsService = SatsService(satsRepository)
        beregningService = BeregningService(andelTilkjentYtelseRepository, fagsakService, tilkjentYtelseRepository, behandlingResultatRepository, satsService)

        every { andelTilkjentYtelseRepository.slettAlleAndelerTilkjentYtelseForBehandling(any()) } just Runs
        every { tilkjentYtelseRepository.slettTilkjentYtelseFor(any()) } just Runs
        every { fagsakService.hentRestFagsak(any()) } answers { Ressurs.success(defaultFagsak.toRestFagsak(emptyList())) }
        every { satsRepository.finnAlleSatserFor(any()) } answers { listOf(Sats(type = SatsType.ORBA, beløp = 1054, gyldigFom = LocalDate.MIN, gyldigTom = LocalDate.MAX)) }
    }

    @Test
    fun `Skal mappe perioderesultat til andel ytelser`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val behandlingResultat = BehandlingResultat(behandling = behandling)

        val periodeFom = LocalDate.of(2020, 1, 1)
        val periodeTom = LocalDate.of(2020, 11, 1)
        val periodeResultatBarn = lagPeriodeResultat(
                barn1Fnr,
                behandlingResultat = behandlingResultat,
                resultat = Resultat.JA,
                periodeFom = periodeTom,
                periodeTom = periodeFom
        )

        val periodeResultatSøker = lagPeriodeResultat(
                søkerFnr,
                behandlingResultat = behandlingResultat,
                resultat = Resultat.JA,
                periodeFom = periodeFom,
                periodeTom = periodeTom
        )
        behandlingResultat.periodeResultater = setOf(periodeResultatBarn, periodeResultatSøker)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søkerFnr, barnasIdenter = listOf(barn1Fnr))
        val slot = slot<TilkjentYtelse>()

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { behandlingResultat }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling, personopplysningGrunnlag = personopplysningGrunnlag, nyBeregning = null)

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(1, slot.captured.andelerTilkjentYtelse.size)
        Assertions.assertEquals(1054, slot.captured.andelerTilkjentYtelse.first().beløp)
        Assertions.assertEquals(periodeFom.plusMonths(1), slot.captured.andelerTilkjentYtelse.first().stønadFom)
        Assertions.assertEquals(periodeTom.plusMonths(1).sisteDagIMåned(), slot.captured.andelerTilkjentYtelse.first().stønadTom)
    }

    @Test
    fun `For flere barn med forskjellige perioderesultat skal perioderesultat mappes til andel ytelser`() {
        val behandling = lagBehandling()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val søkerFnr = randomFnr()
        val behandlingResultat = BehandlingResultat(
                behandling = behandling
        )

        val periode1Fom = LocalDate.of(2020, 1, 1)
        val periode1Tom = LocalDate.of(2020, 11, 1)

        val periode2Fom = LocalDate.of(2020, 12, 1)
        val periode2Midt = LocalDate.of(2021, 6, 1)
        val periode2Tom = LocalDate.of(2021, 12, 11)

        val periode3Fom = LocalDate.of(2022, 1, 12)
        val periode3Midt = LocalDate.of(2023, 6, 1)
        val periode3Tom = LocalDate.of(2028, 1, 1)

        val periodeResultater = mutableSetOf(
                lagPeriodeResultat(
                        søkerFnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = periode1Fom,
                        periodeTom = periode1Tom
                ),
                lagPeriodeResultat(
                        søkerFnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.NEI,
                        periodeFom = periode2Fom,
                        periodeTom = periode2Tom
                ),
                lagPeriodeResultat(
                        søkerFnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = periode3Fom,
                        periodeTom = periode3Tom
                ),
                lagPeriodeResultat(
                        barn1Fnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = periode1Fom.minusYears(1),
                        periodeTom = periode3Tom.plusYears(1)
                ),
                lagPeriodeResultat(
                        barn2Fnr,
                        behandlingResultat = behandlingResultat,
                        resultat = Resultat.JA,
                        periodeFom = periode2Midt,
                        periodeTom = periode3Midt
                )
        )

        behandlingResultat.periodeResultater = periodeResultater

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barn1Fnr, barn2Fnr)
        )
        val slot = slot<TilkjentYtelse>()

        every { behandlingResultatRepository.findByBehandlingAndAktiv(any()) } answers { behandlingResultat }
        every { tilkjentYtelseRepository.save(any<TilkjentYtelse>()) } returns lagInitiellTilkjentYtelse(behandling)

        beregningService.oppdaterBehandlingMedBeregning(behandling = behandling, personopplysningGrunnlag = personopplysningGrunnlag, nyBeregning = null)

        verify(exactly = 1) { tilkjentYtelseRepository.save(capture(slot)) }

        Assertions.assertEquals(3, slot.captured.andelerTilkjentYtelse.size)
        val andelTilkjentYtelser = slot.captured.andelerTilkjentYtelse.sortedBy { it.stønadTom }

        Assertions.assertEquals(periode1Fom.plusMonths(1).withDayOfMonth(1), andelTilkjentYtelser[0].stønadFom)
        Assertions.assertEquals(periode1Tom.plusMonths(1).sisteDagIMåned(), andelTilkjentYtelser[0].stønadTom)
        Assertions.assertEquals(1054, andelTilkjentYtelser[0].beløp)

        Assertions.assertEquals(periode3Fom.plusMonths(1).withDayOfMonth(1), andelTilkjentYtelser[1].stønadFom)
        Assertions.assertEquals(periode3Midt.plusMonths(1).sisteDagIMåned(), andelTilkjentYtelser[1].stønadTom)
        Assertions.assertEquals(1054, andelTilkjentYtelser[1].beløp)

        Assertions.assertEquals(periode3Fom.plusMonths(1).withDayOfMonth(1), andelTilkjentYtelser[2].stønadFom)
        Assertions.assertEquals(periode3Tom.plusMonths(1).sisteDagIMåned(), andelTilkjentYtelser[2].stønadTom)
        Assertions.assertEquals(1054, andelTilkjentYtelser[2].beløp)


    }
}

fun lagPeriodeResultat(fnr: String, resultat: Resultat, periodeFom: LocalDate?, periodeTom: LocalDate?, behandlingResultat: BehandlingResultat): PeriodeResultat {
    val periodeResultat = PeriodeResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr,
            periodeFom = periodeFom,
            periodeTom = periodeTom)
    periodeResultat.vilkårResultater =
            setOf(VilkårResultat(periodeResultat = periodeResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = resultat,
                    begrunnelse = ""))
    return periodeResultat
}