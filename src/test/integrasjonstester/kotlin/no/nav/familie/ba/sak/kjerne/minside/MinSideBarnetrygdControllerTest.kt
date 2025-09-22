package no.nav.familie.ba.sak.kjerne.minside

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.YearMonth

class MinSideBarnetrygdControllerTest(
    @Autowired private val minSideBarnetrygdService: MinSideBarnetrygdService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : AbstractSpringIntegrationTest() {
    private val fnr = randomFnr()

    private val minSideBarnetrygdController = MinSideBarnetrygdController(minSideBarnetrygdService)

    @BeforeEach
    fun setup() {
        mockkObject(EksternBrukerUtils)
        every { EksternBrukerUtils.hentFnrFraToken() } returns fnr
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(EksternBrukerUtils)
    }

    @Nested
    inner class HentMinSideBarnetrygd {
        @Test
        fun `skal returnere response med feil hvis fnr ikke han bli hentet fra token`() {
            // Arrange
            every { EksternBrukerUtils.hentFnrFraToken() } throws JwtTokenValidatorException("Finner ikke token.")

            // Act
            val response = minSideBarnetrygdController.hentMinSideBarnetrygd()

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
            assertThat(response.body).isInstanceOfSatisfying(HentMinSideBarnetrygdDto.Feil::class.java) {
                assertThat(it.feilmelding).isEqualTo("Mangler tilgang.")
            }
        }

        @Test
        fun `skal returnere response med feil hvis fnr fra token ikke kan konverteres til et fødselsnummer`() {
            // Arrange
            every { EksternBrukerUtils.hentFnrFraToken() } returns "12345678903"

            // Act
            val response = minSideBarnetrygdController.hentMinSideBarnetrygd()

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isInstanceOfSatisfying(HentMinSideBarnetrygdDto.Feil::class.java) {
                assertThat(it.feilmelding).isEqualTo("Ugydlig fødselsnummer.")
            }
        }

        @Test
        fun `skal hente min side barnetrygd`() {
            // Arrange
            val andelFom = YearMonth.now().minusMonths(5)
            val andelTom = YearMonth.now().plusMonths(5)

            val aktør = aktørIdRepository.save(randomAktør(fnr = fnr))

            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))

            val behandling =
                behandlingRepository.save(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                    ),
                )

            val tilkjentYtelse =
                tilkjentYtelseRepository.save(
                    lagInitiellTilkjentYtelse(
                        behandling = behandling,
                        utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id, ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD),
                    ),
                )

            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = aktør,
                    fom = andelFom,
                    tom = andelTom,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
            )

            // Act
            val response = minSideBarnetrygdController.hentMinSideBarnetrygd()

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isInstanceOfSatisfying(HentMinSideBarnetrygdDto.Suksess::class.java) {
                assertThat(it.barnetrygd?.ordinær?.startmåned).isEqualTo(andelFom)
                assertThat(it.barnetrygd?.utvidet).isNull()
            }
        }
    }
}
