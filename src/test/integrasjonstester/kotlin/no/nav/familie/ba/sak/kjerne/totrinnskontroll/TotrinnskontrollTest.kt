package no.nav.familie.ba.sak.kjerne.totrinnskontroll

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TotrinnskontrollTest(
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val totrinnskontrollService: TotrinnskontrollService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    @Tag("integration")
    fun `Skal godkjenne 2 trinnskontroll`() {
        // Arrange
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(fagsakId = fagsak.id))

        // Act
        behandlingService.sendBehandlingTilBeslutter(behandling)

        // Assert
        assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        ).hasSize(2)
        assertThat(
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.BEHANDLING,
                    behandling.id,
                ).last()
                .jsonToBehandlingDVH()
                .behandlingStatus,
        ).isEqualTo(BehandlingStatus.FATTER_VEDTAK.name)

        // Act
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)

        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter", "beslutterId", Beslutning.GODKJENT)

        // Assert
        assertEquals(BehandlingStatus.IVERKSETTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)

        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        ).hasSize(3)
        assertThat(
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.BEHANDLING,
                    behandling.id,
                ).last()
                .jsonToBehandlingDVH()
                .behandlingStatus,
        ).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK.name)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertTrue(totrinnskontroll.godkjent)
    }

    @Test
    @Tag("integration")
    fun `Skal underkjenne 2 trinnskontroll`() {
        // Arrange
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(fagsakId = fagsak.id))

        // Act
        behandlingService.sendBehandlingTilBeslutter(behandling)

        // Assert
        assertEquals(BehandlingStatus.FATTER_VEDTAK, behandlingHentOgPersisterService.hent(behandling.id).status)

        // Act
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling = behandling)
        totrinnskontrollService.besluttTotrinnskontroll(behandling, "Beslutter", "beslutterId", Beslutning.UNDERKJENT)

        // Assert
        assertEquals(BehandlingStatus.UTREDES, behandlingHentOgPersisterService.hent(behandling.id).status)
        assertThat(
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(
                SaksstatistikkMellomlagringType.BEHANDLING,
                behandling.id,
            ),
        ).hasSize(3)
        assertThat(
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(
                    SaksstatistikkMellomlagringType.BEHANDLING,
                    behandling.id,
                ).last()
                .jsonToBehandlingDVH()
                .behandlingStatus,
        ).isEqualTo(BehandlingStatus.UTREDES.name)

        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id)!!
        assertFalse(totrinnskontroll.godkjent)
    }

    @Test
    fun `Skal ikke kunne godkjenne eget vedtak`() {
        // Arrange
        val totrinnskontroll =
            Totrinnskontroll(
                behandling = lagBehandlingUtenId(),
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "Mock.Saksbehandler",
                beslutter = "Mock Saksbehandler",
                beslutterId = "Mock.Saksbehandler",
                godkjent = true,
            )

        // Act & Assert
        assertTrue(totrinnskontroll.erUgyldig())
    }

    @Test
    fun `Skal kunne underkjenne eget vedtak`() {
        // Arrange
        val totrinnskontroll =
            Totrinnskontroll(
                behandling = lagBehandlingUtenId(),
                saksbehandler = "Mock Saksbehandler",
                saksbehandlerId = "Mock.Saksbehandler",
                beslutter = "Mock Saksbehandler",
                beslutterId = "Mock.Saksbehandler",
                godkjent = false,
            )

        // Act & Assert
        assertFalse(totrinnskontroll.erUgyldig())
    }
}
