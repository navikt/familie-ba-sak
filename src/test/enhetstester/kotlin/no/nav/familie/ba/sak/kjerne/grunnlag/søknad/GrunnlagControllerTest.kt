package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GrunnlagControllerTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val utvidetBehandlingService = mockk<UtvidetBehandlingService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val tilbakestillService = mockk<TilbakestillBehandlingService>()
    private val tilgangService = mockk<TilgangService>()

    private val grunnlagController =
        GrunnlagController(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            utvidetBehandlingService = utvidetBehandlingService,
            persongrunnlagService = persongrunnlagService,
            tilbakestillService = tilbakestillService,
            tilgangService = tilgangService,
        )

    @Nested
    inner class LeggTilBarnIPersonopplysningsgrunnlag {
        private val behandling = lagBehandling()
        private val barnIdent = randomFnr()
        private val leggTilBarnDto = GrunnlagController.LeggTilBarnDto(barnIdent = barnIdent)

        @BeforeEach
        fun setUp() {
            every { tilgangService.validerTilgangTilBehandling(any(), any()) } just runs
            every { tilgangService.verifiserHarTilgangTilHandling(any(), any()) } just runs
            every { tilgangService.validerTilgangTilPersoner(any(), any(), any()) } just runs
            every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling
            every { persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(any(), any()) } just runs
            every { tilbakestillService.initierOgSettBehandlingTilVilkårsvurdering(any()) } just runs
            every { utvidetBehandlingService.lagUtvidetBehandlingDto(behandling.id) } returns mockk()
        }

        @Test
        fun `skal sjekke tilgang til barnet før det legges til på behandlingen`() {
            // Act
            grunnlagController.leggTilBarnIPersonopplysningsgrunnlag(behandling.id, leggTilBarnDto)

            // Assert
            verifyOrder {
                tilgangService.validerTilgangTilPersoner(
                    personIdenter = listOf(barnIdent),
                    event = AuditLoggerEvent.UPDATE,
                )
                persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(nyttBarnIdent = barnIdent, behandling = behandling)
            }
        }

        @Test
        fun `skal ikke legge til barnet når saksbehandler mangler tilgang til barnet`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilPersoner(
                    personIdenter = listOf(barnIdent),
                    event = AuditLoggerEvent.UPDATE,
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang til barnet")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    grunnlagController.leggTilBarnIPersonopplysningsgrunnlag(behandling.id, leggTilBarnDto)
                }.message

            assertThat(feilmelding).isEqualTo("Mangler tilgang til barnet")
            verify(exactly = 0) { persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(any(), any()) }
        }
    }
}
