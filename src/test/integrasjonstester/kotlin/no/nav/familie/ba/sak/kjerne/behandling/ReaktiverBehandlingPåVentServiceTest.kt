package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReaktiverBehandlingPåVentServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val hentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val reaktiverBehandlingPåVentService: ReaktiverBehandlingPåVentService,
) : AbstractSpringIntegrationTest() {

    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        fagsak = opprettLøpendeFagsak()
    }

    private fun opprettLøpendeFagsak(): Fagsak {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())
        return fagsakService.lagre(fagsak.copy(status = FagsakStatus.LØPENDE))
    }

    @Test
    fun `siste behandlingen er den som er aktiv til at behandlingen som er satt på vent er aktivert på nytt`() {
        opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling2)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert, men ennå ikke iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        reaktiverBehandlingPåVentService.reaktiverBehandlingPåVent(fagsak.id, behandling1.id, behandling2.id)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert og iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        reaktiverBehandlingPåVentService.reaktiverBehandlingPåVent(fagsak.id, behandling1.id, behandling2.id)
        lagUtbetalingsoppdragOgAvslutt(behandling1)

        validerSisteBehandling(behandling1)
        validerErAktivBehandling(behandling1)
    }

    @Nested
    inner class ValideringAvBehandlinger {

        @Test
        fun `skal feile når åpen behandling er aktiv`() {
            val åpenBehandling = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = true)
            val behandlingSomSniketIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)

            assertThatThrownBy { reaktiverBehandling(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("Åpen behandling har feil tilstand")
        }

        @Test
        fun `skal feile når åpen behandling ikke har status SATT_PÅ_VENT`() {
            BehandlingStatus.values().filter { it != BehandlingStatus.SATT_PÅ_VENT }.forEach {
                behandlingRepository.deleteAll()
                val åpenBehandling = opprettBehandling(status = it, aktiv = false)
                val behandlingSomSniketIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

                assertThatThrownBy { reaktiverBehandling(åpenBehandling, behandlingSomSniketIKøen) }
                    .hasMessageContaining("Åpen behandling har feil tilstand")
            }
        }

        @Test
        fun `skal feile når behandling som sniker i køen ikke er aktiv`() {
            val åpenBehandling = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
            val behandlingSomSniketIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)

            assertThatThrownBy { reaktiverBehandling(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("Behandling som sniket i køen må være aktiv")
        }

        @Test
        fun `skal feile når behandling som sniker i køen har status satt på vent`() {
            val åpenBehandling = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
            val behandlingSomSniketIKøen = opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true)

            assertThatThrownBy { reaktiverBehandling(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("er ikke avsluttet")
        }
    }

    private fun reaktiverBehandling(
        åpenBehandling: Behandling,
        behandlingSomSniketIKøen: Behandling,
    ) {
        reaktiverBehandlingPåVentService.reaktiverBehandlingPåVent(
            fagsak.id,
            åpenBehandling.id,
            behandlingSomSniketIKøen.id,
        )
    }

    private fun validerSisteBehandling(behandling: Behandling) {
        val fagsakId = fagsak.id
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()).containsExactly(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!.id).isEqualTo(behandling.id)

        assertThat(hentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(hentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(
            hentOgPersisterService.hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(setOf(fagsakId)).single().id,
        ).isEqualTo(behandling.id)
    }

    private fun validerErAktivBehandling(behandling: Behandling) {
        assertThat(hentOgPersisterService.finnAktivForFagsak(behandling.fagsak.id)!!.id)
            .isEqualTo(behandling.id)
    }

    private fun lagUtbetalingsoppdragOgAvslutt(behandling: Behandling) {
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling, utbetalingsoppdrag = "utbetalingsoppdrag")
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
        behandlingRepository.finnBehandling(behandling.id).let { behandling ->
            val stegTilstand =
                BehandlingStegTilstand(behandling = behandling, behandlingSteg = StegType.BEHANDLING_AVSLUTTET)
            behandling.behandlingStegTilstand.add(stegTilstand)
            behandling.status = BehandlingStatus.AVSLUTTET
            behandlingRepository.save(behandling)
        }
    }

    private fun opprettBehandling(status: BehandlingStatus, aktiv: Boolean): Behandling {
        return opprettBehandling(fagsak, status, aktiv)
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus,
        aktiv: Boolean,
    ): Behandling {
        val behandling = Behandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            type = BehandlingType.REVURDERING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            status = status,
            aktiv = aktiv,
        ).initBehandlingStegTilstand()
        return behandlingRepository.save(behandling)
    }
}
