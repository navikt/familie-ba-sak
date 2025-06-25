package no.nav.familie.ba.sak.kjerne.minside

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class MinSideBarnetrygdServiceTest {
    private val fødselsnummer = Fødselsnummer(randomFnr())
    private val aktør = randomAktør(fnr = fødselsnummer.verdi)
    private val fagsak = lagFagsak(aktør = aktør)
    private val behandling = lagBehandling(fagsak = fagsak)

    private val personidentService = mockk<PersonidentService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val minSideBarnetrygdService =
        MinSideBarnetrygdService(
            personidentService = personidentService,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(any()) } returns aktør
        every { fagsakService.hentFagsakPåPerson(eq(aktør), eq(FagsakType.NORMAL)) } returns fagsak
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(eq(fagsak.id)) } returns behandling
    }

    @Nested
    inner class HentMinSideBarnetrygd {
        @Test
        fun `skal returnere tom barnetrygd hvis fagsak er null`() {
            // Arrange
            every { fagsakService.hentFagsakPåPerson(eq(aktør), eq(FagsakType.NORMAL)) } returns null

            // Act
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fødselsnummer)

            // Assert
            assertThat(minSideBarnetrygd?.ordinær).isNull()
            assertThat(minSideBarnetrygd?.utvidet).isNull()
        }

        @Test
        fun `skal returnere tom barnetrygd hvis behandling er null`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(eq(fagsak.id)) } returns null

            // Act
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fødselsnummer)

            // Assert
            assertThat(minSideBarnetrygd?.ordinær).isNull()
            assertThat(minSideBarnetrygd?.utvidet).isNull()
        }

        @Test
        fun `skal returnere tom barnetrygd hvis andeler tilkjent ytelser er tom`() {
            // Arrange
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(eq(behandling.id)) } returns emptyList()

            // Act
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fødselsnummer)

            // Assert
            assertThat(minSideBarnetrygd?.ordinær).isNull()
            assertThat(minSideBarnetrygd?.utvidet).isNull()
        }

        @Test
        fun `skal returnere innvilget ordinær og utvidet barnetrygd`() {
            // Arrange
            val andeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 3),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 4),
                        tom = YearMonth.of(2025, 7),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 3),
                        tom = YearMonth.of(2025, 4),
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    ),
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 5),
                        tom = YearMonth.of(2025, 6),
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(eq(behandling.id)) } returns andeler

            // Act
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fødselsnummer)

            // Assert
            assertThat(minSideBarnetrygd?.ordinær?.startmåned).isEqualTo(YearMonth.of(2025, 1))
            assertThat(minSideBarnetrygd?.utvidet?.startmåned).isEqualTo(YearMonth.of(2025, 3))
        }

        @Test
        fun `skal returnere innvilget ordinær barnetrygd`() {
            // Arrange
            val andeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 3),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                    lagAndelTilkjentYtelse(
                        behandling = behandling,
                        aktør = aktør,
                        fom = YearMonth.of(2025, 4),
                        tom = YearMonth.of(2025, 7),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(eq(behandling.id)) } returns andeler

            // Act
            val minSideBarnetrygd = minSideBarnetrygdService.hentMinSideBarnetrygd(fødselsnummer)

            // Assert
            assertThat(minSideBarnetrygd?.ordinær?.startmåned).isEqualTo(YearMonth.of(2025, 1))
            assertThat(minSideBarnetrygd?.utvidet).isNull()
        }
    }
}
