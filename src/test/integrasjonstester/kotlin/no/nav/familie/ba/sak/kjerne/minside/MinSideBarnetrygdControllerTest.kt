package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.time.YearMonth

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
)
class MinSideBarnetrygdControllerTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : WebSpringAuthTestRunner() {
    @Nested
    inner class HentMinSideBarnetrygd {
        @Test
        fun `skal returnere UNAUTHORIZED når request mangler token`() {
            val error =
                assertThrows<HttpClientErrorException> {
                    restTemplate.exchange<String>(
                        hentUrl("/api/minside/barnetrygd"),
                        HttpMethod.GET,
                        HttpEntity<String>(HttpHeaders()),
                    )
                }

            assertThat(error.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `skal returnere FORBIDDEN når token mangler fødselsnummer`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(hentTokenForTokenX(null))
                }

            val error =
                assertThrows<HttpClientErrorException> {
                    restTemplate.exchange<String>(
                        hentUrl("/api/minside/barnetrygd"),
                        HttpMethod.GET,
                        HttpEntity<String>(headers),
                    )
                }

            assertThat(error.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }

        @Test
        fun `skal returnere BAD_REQUEST når token inneholder ugyldig fødselsnummer`() {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(hentTokenForTokenX("12345678910"))
                }

            val error =
                assertThrows<HttpClientErrorException> {
                    restTemplate.exchange<String>(
                        hentUrl("/api/minside/barnetrygd"),
                        HttpMethod.GET,
                        HttpEntity<String>(headers),
                    )
                }

            assertThat(error.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `skal hente min side barnetrygd med TokenX-token`() {
            val fnr = randomFnr()
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
                        utbetalingsoppdrag =
                            lagMinimalUtbetalingsoppdragString(
                                behandlingId = behandling.id,
                                ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            ),
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

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(hentTokenForTokenX(fnr))
                }

            val response =
                restTemplate.exchange<HentMinSideBarnetrygdDto.Suksess>(
                    hentUrl("/api/minside/barnetrygd"),
                    HttpMethod.GET,
                    HttpEntity<String>(headers),
                )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(
                response.body
                    ?.barnetrygd
                    ?.ordinær
                    ?.startmåned,
            ).isEqualTo(andelFom)
            assertThat(response.body?.barnetrygd?.utvidet).isNull()
        }
    }
}
