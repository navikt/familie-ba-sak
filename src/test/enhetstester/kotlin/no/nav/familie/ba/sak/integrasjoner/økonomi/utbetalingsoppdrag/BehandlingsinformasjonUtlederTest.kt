package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BehandlingsinformasjonUtlederTest {
    private val clock: Clock =
        Clock.fixed(
            Instant.parse("2024-11-01T10:00:00Z"),
            ZoneId.of("Europe/Oslo"),
        )
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder = mockk()
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder =
        BehandlingsinformasjonUtleder(
            endretMigreringsdatoUtleder,
            clock,
        )

    @Test
    fun `skal utlede minimal behandlingsinformasjon`() {
        // Arrange
        val saksbehandlerId = "123"
        val vedtak =
            lagVedtak(
                vedtaksdato = null,
            )
        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }
}
