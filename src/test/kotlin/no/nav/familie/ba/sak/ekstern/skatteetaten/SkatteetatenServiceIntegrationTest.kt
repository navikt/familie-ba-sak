package no.nav.familie.ba.sak.ekstern.skatteetaten

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class SkatteetatenServiceIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val infotrygdBarnetrygdClientMock = mockk<InfotrygdBarnetrygdClient>()

    lateinit var skatteetatenService: SkatteetatenService

    @BeforeEach
    fun cleanUp() {
        databaseCleanupService.truncate()
    }

    @BeforeAll
    fun init() {
        skatteetatenService = SkatteetatenService(infotrygdBarnetrygdClientMock, fagsakRepository, andelTilkjentYtelseRepository)
    }

    data class PerioderTestData(
        val fnr: String,
        val endretDato: LocalDateTime,
        val perioder: List<Triple<LocalDateTime, LocalDateTime, SkatteetatenPeriode.Delingsprosent>>
    )

    @Test
    fun `finnPerioderMedUtvidetBarnetrygd() skal return riktig data`() {
        val duplicatedFnr = "00000000001"
        val excludedFnr = "10000000004"

        //Result from ba-sak
        val testDataBaSak = arrayOf(
            //Excluded because of the vedtak is older
            PerioderTestData(
                fnr = duplicatedFnr,
                endretDato = LocalDateTime.of(2020, 11, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 10, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
            //Included
            PerioderTestData(
                fnr = duplicatedFnr,
                endretDato = LocalDateTime.of(2020, 11, 6, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 9, 1, 12, 0),
                        LocalDateTime.of(2020, 7, 31, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0
                    ),
                    Triple(
                        LocalDateTime.of(2020, 8, 1, 12, 0),
                        LocalDateTime.of(2020, 12, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._50
                    )
                )
            ),
            //Excluded because the stonad period is earlier than the specified year
            PerioderTestData(
                fnr = "00000000002",
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2019, 3, 1, 12, 0),
                        LocalDateTime.of(2019, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
            //Excluded because the stonad period is later than the specified year
            PerioderTestData(
                fnr = "00000000003",
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2021, 1, 1, 1, 0),
                        LocalDateTime.of(2022, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
            //Excluded because the person ident is not in the provided list
            PerioderTestData(
                fnr = excludedFnr,
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 1, 1, 1, 0),
                        LocalDateTime.of(2022, 12, 31, 23, 59),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
        )

        //result from Infotrygd
        val testDataInfotrygd = arrayOf(
            //Excluded because the person ident can be found in ba-sak
            PerioderTestData(
                fnr = duplicatedFnr,
                endretDato = LocalDateTime.of(2020, 9, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 1, 1, 12, 0),
                        LocalDateTime.of(2020, 9, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
            //Included
            PerioderTestData(
                fnr = "00000000010",
                endretDato = LocalDateTime.of(2020, 8, 5, 12, 0),
                perioder = listOf(
                    Triple(
                        LocalDateTime.of(2020, 3, 1, 12, 0),
                        LocalDateTime.of(2020, 4, 8, 12, 0),
                        SkatteetatenPeriode.Delingsprosent._0
                    )
                )
            ),
        )

        testDataBaSak.forEach {
            every {
                infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygd(
                    eq(it.fnr),
                    any()
                )
            } returns null

            lagerTilkjentYtelse(it)
        }

        testDataInfotrygd.forEach {
            every {
                infotrygdBarnetrygdClientMock.hentPerioderMedUtvidetBarnetrygd(
                    eq(it.fnr),
                    any()
                )
            } returns SkatteetatenPerioder(
                it.fnr, it.endretDato, it.perioder.map { p ->
                    SkatteetatenPeriode(
                        fraMaaned = p.first.tilMaaned(),
                        tomMaaned = p.second.tilMaaned(),
                        delingsprosent = p.third
                    )
                }
            )
        }

        val samletResultat = skatteetatenService.finnPerioderMedUtvidetBarnetrygd(testDataBaSak.filter { it.fnr != excludedFnr }
                                                                                      .map { it.fnr }
                                                                                          + testDataInfotrygd.map { it.fnr },
                                                                                  "2020"
        )

        assertThat(samletResultat.brukere).hasSize(2)
        assertThat(samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder).hasSize(2)
        assertThat(samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder.find {
            it.fraMaaned == "2020-08"
        }!!.delingsprosent).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._50
        )
        assertThat(samletResultat.brukere.find { it.ident == duplicatedFnr }!!.perioder.find {
            it.tomMaaned == "2020-07"
        }!!.delingsprosent).isEqualTo(
            SkatteetatenPeriode.Delingsprosent._0
        )
        assertThat(samletResultat.brukere.find { it.ident == testDataInfotrygd[1].fnr }!!.perioder).hasSize(1)
    }

    fun lagerTilkjentYtelse(tilkjentYtelse: PerioderTestData) {
        val fagsak = Fagsak()
        fagsakRepository.saveAndFlush(fagsak)

        val behandling = Behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            opprettetÅrsak = BehandlingÅrsak.MIGRERING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.UTVIDET,
        )
        behandlingRepository.saveAndFlush(behandling)

        val ty = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = tilkjentYtelse.endretDato.toLocalDate(),
            endretDato = tilkjentYtelse.endretDato.toLocalDate(),
            utbetalingsoppdrag = "utbetalt",
        ).also {
            it.andelerTilkjentYtelse.addAll(tilkjentYtelse.perioder.map { p ->
                AndelTilkjentYtelse(
                    behandlingId = it.behandling.id,
                    tilkjentYtelse = it,
                    personIdent = tilkjentYtelse.fnr,
                    kalkulertUtbetalingsbeløp = 1000,
                    stønadFom = YearMonth.of(p.first.year, p.first.month),
                    stønadTom = YearMonth.of(p.second.year, p.second.month),
                    type = YtelseType.UTVIDET_BARNETRYGD,
                    sats = 1,
                    prosent = p.third.tilBigDecimal()
                )
            }.toMutableSet())
        }
        tilkjentYtelseRepository.saveAndFlush(ty)
    }
}

fun LocalDateTime.tilMaaned(): String = this.format(DateTimeFormatter.ofPattern("YYYY-MM"))