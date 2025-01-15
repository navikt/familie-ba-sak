package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import lagBehandling
import lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagPersonResultaterForSøkerOgToBarn
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagValutakurs
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import randomFnr
import java.math.BigDecimal
import java.time.LocalDate

class BeregningServiceIntegrationTest : AbstractSpringIntegrationTest() {
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

    @Autowired
    private lateinit var personidentService: PersonidentService

    @Autowired
    private lateinit var kompetanseRepository: KompetanseRepository

    @Autowired
    private lateinit var utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository

    @Autowired
    private lateinit var valutakursRepository: ValutakursRepository

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal lagre andelerTilkjentYtelse med kobling til TilkjentYtelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val søkerAktørId = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barn1AktørId = personidentService.hentOgLagreAktør(barn1Fnr, true)
        val barn2AktørId = personidentService.hentOgLagreAktør(barn2Fnr, true)
        val dato20211101 = LocalDate.of(2021, 11, 1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barn1Fnr, barn2Fnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val barn1Id =
            personopplysningGrunnlag.barna
                .find { it.aktør.aktivFødselsnummer() == barn1Fnr }!!
                .aktør
                .aktivFødselsnummer()
        val barn2Id =
            personopplysningGrunnlag.barna
                .find { it.aktør.aktivFødselsnummer() == barn2Fnr }!!
                .aktør
                .aktivFødselsnummer()

        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater =
            lagPersonResultaterForSøkerOgToBarn(
                vilkårsvurdering,
                søkerAktørId,
                barn1AktørId,
                barn2AktørId,
                dato20211101,
                dato20211101.plusYears(17),
            )
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val andelBarn1 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn1Id }
        val andelBarn2 = tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktivFødselsnummer() == barn2Id }

        Assertions.assertNotNull(tilkjentYtelse)
        Assertions.assertTrue(tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty())
        Assertions.assertEquals(3, andelBarn1.size)
        Assertions.assertEquals(3, andelBarn2.size)
        tilkjentYtelse.andelerTilkjentYtelse.forEach {
            Assertions.assertEquals(tilkjentYtelse, it.tilkjentYtelse)
        }
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1654 }.size)
        Assertions.assertEquals(1, andelBarn1.filter { it.kalkulertUtbetalingsbeløp == 1676 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1054 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1654 }.size)
        Assertions.assertEquals(1, andelBarn2.filter { it.kalkulertUtbetalingsbeløp == 1676 }.size)
    }

    @Test
    @Transactional
    fun `genererTilkjentYtelseFraVilkårsvurdering - skal trigge differanseberegning`() {
        val søker = personidentService.hentOgLagreAktør(randomFnr(), true)
        val barn = personidentService.hentOgLagreAktør(randomFnr(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktivFødselsnummer())
        val behandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(
                behandling =
                    lagBehandling(
                        fagsak = fagsak,
                        behandlingKategori = BehandlingKategori.EØS,
                        skalBehandlesAutomatisk = true,
                    ),
            )

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søker.aktivFødselsnummer(),
                listOf(barn.aktivFødselsnummer()),
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val vilkårsvurdering =
            Vilkårsvurdering(id = 0, behandling = behandling, aktiv = true).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = personopplysningGrunnlag.søker,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(4),
                            periodeTom = LocalDate.now(),
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.SØKER,
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = personopplysningGrunnlag.barna.first(),
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(4),
                            periodeTom = LocalDate.now(),
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                        ),
                    )
            }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        val kompetanseNorgeErSekundærland =
            lagKompetanse(
                behandlingId = behandling.id,
                fom = LocalDate.now().minusMonths(4).toYearMonth(),
                barnAktører = setOf(barn),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitetsland = "Sverige",
                søkersAktivitetsland = "Norge",
                barnetsBostedsland = "Norge",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        val utenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = behandling.id,
                fom = LocalDate.now().minusMonths(4).toYearMonth(),
                barnAktører = setOf(barn),
                beløp = BigDecimal.valueOf(1000),
                intervall = Intervall.MÅNEDLIG,
                valutakode = "SEK",
                utbetalingsland = "Sverige",
            )
        val valutakurs =
            lagValutakurs(
                behandlingId = behandling.id,
                fom = LocalDate.now().minusMonths(4).toYearMonth(),
                barnAktører = setOf(barn),
                valutakursdato = LocalDate.now(),
                valutakode = "SEK",
                kurs = BigDecimal.valueOf(1.1),
            )

        kompetanseRepository.save(kompetanseNorgeErSekundærland)
        utenlandskPeriodebeløpRepository.save(utenlandskPeriodebeløp)
        valutakursRepository.save(valutakurs)

        val tilkjentYtelse = beregningService.genererTilkjentYtelseFraVilkårsvurdering(behandling = behandling, personopplysningGrunnlag = personopplysningGrunnlag)

        assertThat(tilkjentYtelse.andelerTilkjentYtelse.any { it.differanseberegnetPeriodebeløp != null }).isEqualTo(true)
    }
}
