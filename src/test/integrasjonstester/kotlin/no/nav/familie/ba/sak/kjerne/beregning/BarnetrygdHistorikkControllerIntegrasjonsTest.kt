package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.toEntity
import java.time.YearMonth

@ActiveProfiles(
    "postgres",
    "integrasjonstest",
    "testcontainers",
    "mock-pdl",
    "mock-pdl-klient",
    "mock-ident-klient",
    "mock-infotrygd-barnetrygd",
    "fake-tilbakekreving-klient",
    "mock-brev-klient",
    "fake-økonomi-klient",
    "mock-infotrygd-feed",
    "mock-task-repository",
    "mock-task-service",
    "mock-unleash",
)
class BarnetrygdHistorikkControllerIntegrasjonsTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : WebSpringAuthTestRunner() {
    @Test
    fun `skal returnere true for andel med nullutbetaling når system kaller endepunktet`() {
        // Arrange
        val fagsak = lagreFagsak()
        lagreBehandlingMedAndel(
            fagsak = fagsak,
            status = BehandlingStatus.AVSLUTTET,
            resultat = Behandlingsresultat.INNVILGET,
            kalkulertUtbetalingsbeløp = 0,
        )

        // Act
        val response = kallHarSøkerHattUtbetaling(fagsakId = fagsak.id, headers = hentHeadersForSystembruker())

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data).isTrue()
    }

    @Test
    fun `skal returnere false når søker bare har andeler på pågående behandling`() {
        // Arrange
        val fagsak = lagreFagsak()
        lagreBehandlingMedAndel(fagsak = fagsak, status = BehandlingStatus.UTREDES, resultat = Behandlingsresultat.IKKE_VURDERT)

        // Act
        val response = kallHarSøkerHattUtbetaling(fagsakId = fagsak.id, headers = hentHeadersForSystembruker())

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isFalse()
    }

    @Test
    fun `skal returnere false når søker bare har andeler på henlagt behandling`() {
        // Arrange
        val fagsak = lagreFagsak()
        lagreBehandlingMedAndel(
            fagsak = fagsak,
            status = BehandlingStatus.AVSLUTTET,
            resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
        )

        // Act
        val response = kallHarSøkerHattUtbetaling(fagsakId = fagsak.id, headers = hentHeadersForSystembruker())

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data).isFalse()
    }

    @Test
    fun `skal avvise kall uten systemtilgang`() {
        // Act & Assert
        val feil =
            assertThrows<HttpClientErrorException> {
                kallHarSøkerHattUtbetaling(fagsakId = 1L, headers = hentHeaders())
            }

        assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    private fun kallHarSøkerHattUtbetaling(
        fagsakId: Long,
        headers: HttpHeaders,
    ): ResponseEntity<Ressurs<Boolean>> =
        restClient
            .get()
            .uri(hentUrl("/api/tilkjentytelse/fagsak/$fagsakId/soker-har-hatt-utbetaling"))
            .headers { it.addAll(headers) }
            .retrieve()
            .toEntity<Ressurs<Boolean>>()

    private fun lagreFagsak(): Fagsak {
        val aktør = aktørIdRepository.save(randomAktør())
        return fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
    }

    private fun lagreBehandlingMedAndel(
        fagsak: Fagsak,
        status: BehandlingStatus,
        resultat: Behandlingsresultat,
        kalkulertUtbetalingsbeløp: Int? = null,
    ) {
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = status, resultat = resultat))
        val tilkjentYtelse = tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling = behandling))
        andelTilkjentYtelseRepository.save(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 12),
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                aktør = fagsak.aktør,
                kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
            ),
        )
    }
}
