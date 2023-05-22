package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
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
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReaktiverÅpenBehandlingTaskTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val reaktiverÅpenBehandlingTask: ReaktiverÅpenBehandlingTask,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val hentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
) : AbstractSpringIntegrationTest() {

    private val person = tilfeldigPerson()
    private val person2 = tilfeldigPerson()

    private lateinit var fagsak: Fagsak
    private lateinit var fagsak2: Fagsak

    private lateinit var åpenBehandling: Behandling
    private lateinit var behandlingSomSniketIKøen: Behandling

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(person.aktør.aktivFødselsnummer())
        fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(person2.aktør.aktivFødselsnummer())

        åpenBehandling = opprettBehandling(status = BehandlingStatus.SATT_PÅ_VENT, aktiv = false)
        behandlingSomSniketIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
    }

    @Test
    fun `skal aktivere gammel og deaktivere behandling som sniket`() {
        kjørTask(åpenBehandling, behandlingSomSniketIKøen)

        val oppdatertÅpenBehandling = behandlingRepository.finnBehandling(åpenBehandling.id)
        assertThat(oppdatertÅpenBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(oppdatertÅpenBehandling.aktiv).isTrue()

        val oppdatertSniketBehandling = behandlingRepository.finnBehandling(behandlingSomSniketIKøen.id)
        assertThat(oppdatertSniketBehandling.status).isEqualTo(BehandlingStatus.AVSLUTTET)
        assertThat(oppdatertSniketBehandling.aktiv).isFalse()

        assertThat(oppdatertÅpenBehandling.aktivertTidspunkt).isAfter(oppdatertSniketBehandling.aktivertTidspunkt)
    }

    @Test
    fun `behandling som sniket i køen er siste iverksatte behandlingen`() {
        kjørTask(åpenBehandling, behandlingSomSniketIKøen)

        lagUtbetalingsoppdragOgAvslutt(behandlingSomSniketIKøen)

        validerAktivBehandling(åpenBehandling)
        validerSisteBehandling(behandlingSomSniketIKøen)
    }

    @Test
    fun `behandling som er satt på vent er siste iverksatte behandlingen etter at den har blitt iverksatt og avsluttet`() {
        kjørTask(åpenBehandling, behandlingSomSniketIKøen)

        lagUtbetalingsoppdragOgAvslutt(behandlingSomSniketIKøen)
        lagUtbetalingsoppdragOgAvslutt(åpenBehandling)

        validerAktivBehandling(åpenBehandling)
        validerSisteBehandling(åpenBehandling)
    }

    private fun validerSisteBehandling(behandling: Behandling) {
        val fagsakId = fagsak.id
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(hentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(hentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)!!.id).isEqualTo(behandling.id)
        assertThat(hentOgPersisterService.hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(setOf(fagsakId)).single().id)
            .isEqualTo(behandling.id)
    }

    private fun validerAktivBehandling(behandling: Behandling) {
        val fagsakId = fagsak.id
        assertThat(hentOgPersisterService.finnAktivForFagsak(fagsakId)!!.id).isEqualTo(behandling.id)
    }

    @Nested
    inner class ValideringAvBehandlinger {

        @Test
        fun `skal feile når behandlingene ikke er koblet til samme fagsak`() {
            val åpenBehandling = lagBehandling(fagsak)
            val behandlingSomSniketIKøen = lagBehandling(fagsak2)
            assertThatThrownBy { ReaktiverÅpenBehandlingTask.opprettTask(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("Behandlinger er koblet til ulike fagsaker")
        }

        @Test
        fun `skal feile når åpen behandling er aktiv`() {
            val åpenBehandling = lagBehandling(fagsak)
            val behandlingSomSniketIKøen = lagBehandling(fagsak)
            assertThatThrownBy { ReaktiverÅpenBehandlingTask.opprettTask(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("Åpen behandling har feil state")
        }

        @Test
        fun `skal feile når åpen behandling ikke har status SATT_PÅ_VENT`() {
            val behandlingSomSniketIKøen = lagBehandling(fagsak)
            BehandlingStatus.values().filter { it != BehandlingStatus.SATT_PÅ_VENT }.forEach {
                val åpenBehandling = lagBehandling(fagsak, status = it)
                assertThatThrownBy { ReaktiverÅpenBehandlingTask.opprettTask(åpenBehandling, behandlingSomSniketIKøen) }
                    .hasMessageContaining("Åpen behandling har feil state")
            }
        }

        @Test
        fun `skal feile når behandling som sniker i køen ikke er aktiv`() {
            val åpenBehandling = lagBehandling(fagsak, status = BehandlingStatus.SATT_PÅ_VENT).copy(aktiv = false)
            val behandlingSomSniketIKøen = lagBehandling(fagsak).copy(aktiv = false)

            assertThatThrownBy { ReaktiverÅpenBehandlingTask.opprettTask(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("Behandling som sniket i køen må være aktiv")
        }

        @Test
        fun `skal feile når behandling som sniker i køen har status satt på vent`() {
            val åpenBehandling = lagBehandling(fagsak, status = BehandlingStatus.SATT_PÅ_VENT).copy(aktiv = false)
            val behandlingSomSniketIKøen =
                lagBehandling(fagsak).copy(aktiv = true, status = BehandlingStatus.SATT_PÅ_VENT)

            assertThatThrownBy { ReaktiverÅpenBehandlingTask.opprettTask(åpenBehandling, behandlingSomSniketIKøen) }
                .hasMessageContaining("som sniket i køen kan ikke ha status satt på vent")
        }
    }

    private fun opprettBehandling(status: BehandlingStatus, aktiv: Boolean): Behandling {
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

    private fun kjørTask(
        åpenBehandling: Behandling,
        behandlingSomSniketIKøen: Behandling,
    ) {
        val task = ReaktiverÅpenBehandlingTask.opprettTask(
            åpenBehandling,
            behandlingSomSniketIKøen,
        )
        reaktiverÅpenBehandlingTask.doTask(task)
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
}
