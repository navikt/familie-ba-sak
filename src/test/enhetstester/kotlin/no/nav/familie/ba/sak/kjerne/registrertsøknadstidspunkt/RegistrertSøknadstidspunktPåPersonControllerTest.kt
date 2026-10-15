package no.nav.familie.ba.sak.kjerne.registrertsøknadstidspunkt

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.ekstern.restDomene.EndreSøknadstidspunktRequestDto
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrertSøknadstidspunktPåPersonDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class RegistrertSøknadstidspunktPåPersonControllerTest {
    private val mockTilgangService = mockk<TilgangService>()
    private val mockBehandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val mockRegistrertSøknadstidspunktService = mockk<RegistrertSøknadstidspunktPåPersonService>()
    private val mockEndretUtbetalingAndelService = mockk<EndretUtbetalingAndelService>()
    private val mockUtvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val mockTilbakestillBehandlingTilBehandlingsresultatService = mockk<TilbakestillBehandlingTilBehandlingsresultatService>()

    private val registrertSøknadstidspunktPåPersonController =
        RegistrertSøknadstidspunktPåPersonController(
            tilgangService = mockTilgangService,
            behandlingHentOgPersisterService = mockBehandlingHentOgPersisterService,
            registrertSøknadstidspunktService = mockRegistrertSøknadstidspunktService,
            endretUtbetalingAndelService = mockEndretUtbetalingAndelService,
            utvidetBehandlingService = mockUtvidetBehandlingService,
            tilbakestillBehandlingTilBehandlingsresultatService = mockTilbakestillBehandlingTilBehandlingsresultatService,
        )

    @BeforeEach
    fun setUp() {
        justRun { mockTilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { mockTilgangService.verifiserHarTilgangTilHandling(any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"])
    fun `skal hente registrerte søknadstidspunkt for revurdering etter klage`(årsak: BehandlingÅrsak) {
        // Arrange
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING, årsak = årsak)
        val barn = lagPerson(type = PersonType.BARN)
        val søknadstidspunkt = LocalDate.of(2025, 4, 15)

        every { mockBehandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        every { mockRegistrertSøknadstidspunktService.hentForBehandling(behandling.id) } returns
            listOf(RegistrertSøknadstidspunktPåPerson(behandlingId = behandling.id, aktør = barn.aktør, søknadstidspunkt = søknadstidspunkt))

        // Act
        val respons = registrertSøknadstidspunktPåPersonController.hentRegistrertSøknadstidspunktPåPersoner(behandling.id)

        // Assert
        assertThat(respons.body?.data).containsExactly(
            RegistrertSøknadstidspunktPåPersonDto(personIdent = barn.aktør.aktivFødselsnummer(), søknadstidspunkt = søknadstidspunkt),
        )
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"])
    fun `skal endre søknadstidspunkt, generere etterbetalingsperioder og tilbakestille til behandlingsresultat for revurdering etter klage`(årsak: BehandlingÅrsak) {
        // Arrange
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING, årsak = årsak)
        val søknadstidspunkt = LocalDate.of(2025, 4, 15)

        every { mockBehandlingHentOgPersisterService.hent(behandling.id) } returns behandling
        justRun { mockEndretUtbetalingAndelService.endreSøknadstidspunktOgGenererEtterbetalingsandeler(any(), any()) }
        every { mockTilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id) } returns behandling
        every { mockUtvidetBehandlingService.lagUtvidetBehandlingDto(behandling.id) } returns mockk()

        // Act
        registrertSøknadstidspunktPåPersonController.endreSøknadstidspunkter(
            behandlingId = behandling.id,
            request =
                EndreSøknadstidspunktRequestDto(
                    søknadstidspunktPerPerson = listOf(RegistrertSøknadstidspunktPåPersonDto(personIdent = "12345678910", søknadstidspunkt = søknadstidspunkt)),
                ),
        )

        // Assert
        verifyOrder {
            mockEndretUtbetalingAndelService.endreSøknadstidspunktOgGenererEtterbetalingsandeler(
                behandling = behandling,
                søknadstidspunktPerPerson = listOf(RegistrertSøknadstidspunkt(personIdent = "12345678910", søknadstidspunkt = søknadstidspunkt)),
            )
            mockTilbakestillBehandlingTilBehandlingsresultatService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)
        }
    }

    @Test
    fun `skal ikke kunne endre søknadstidspunkt når behandlingen verken er søknad eller revurdering etter klage`() {
        // Arrange
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING, årsak = BehandlingÅrsak.NYE_OPPLYSNINGER)

        every { mockBehandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        // Act & Assert
        val feil =
            assertThrows<FunksjonellFeil> {
                registrertSøknadstidspunktPåPersonController.endreSøknadstidspunkter(
                    behandlingId = behandling.id,
                    request =
                        EndreSøknadstidspunktRequestDto(
                            søknadstidspunktPerPerson = listOf(RegistrertSøknadstidspunktPåPersonDto(personIdent = "12345678910", søknadstidspunkt = LocalDate.of(2025, 4, 15))),
                        ),
                )
            }

        assertThat(feil.message).isEqualTo("Søknadstidspunkt kan kun registreres for søknadsbehandlinger og revurderinger etter klage.")
        verify(exactly = 0) { mockEndretUtbetalingAndelService.endreSøknadstidspunktOgGenererEtterbetalingsandeler(any(), any()) }
    }
}
