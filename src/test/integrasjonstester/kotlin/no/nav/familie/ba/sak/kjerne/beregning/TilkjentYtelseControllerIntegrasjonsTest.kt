package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.toEntity
import java.time.YearMonth

@ActiveProfiles("postgres", "integrasjonstest", "testcontainers")
class TilkjentYtelseControllerIntegrasjonsTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : WebSpringAuthTestRunner() {
    @Test
    fun `skal returnere true for andel med nullutbetaling når system kaller endepunktet`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
        val tilkjentYtelse = tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling = behandling))
        andelTilkjentYtelseRepository.save(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 12),
                beløp = 0,
                behandling = behandling,
                tilkjentYtelse = tilkjentYtelse,
                aktør = aktør,
            ),
        )

        // Act
        val response =
            restClient
                .get()
                .uri(hentUrl("/api/tilkjentytelse/fagsak/${fagsak.id}/soker-har-hatt-utbetaling"))
                .headers { it.addAll(hentHeadersForSystembruker()) }
                .retrieve()
                .toEntity<Ressurs<Boolean>>()

        // Assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(response.body?.data).isTrue()
    }

    @Test
    fun `skal avvise kall uten systemtilgang`() {
        // Act & Assert
        val feil =
            assertThrows<HttpClientErrorException> {
                restClient
                    .get()
                    .uri(hentUrl("/api/tilkjentytelse/fagsak/1/soker-har-hatt-utbetaling"))
                    .headers { it.addAll(hentHeaders()) }
                    .retrieve()
                    .toEntity<Ressurs<Boolean>>()
            }

        assertThat(feil.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
