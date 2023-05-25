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
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SnikeIKøenServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val hentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
    @Autowired private val settPåVentService: SettPåVentService,
) : AbstractSpringIntegrationTest() {

    private lateinit var fagsak: Fagsak
    private var skalVenteLitt = false // for å unngå at behandlingen opprettes med samme tidspunkt

    @BeforeEach
    fun setUp() {
        skalVenteLitt = false
        databaseCleanupService.truncate()
        fagsak = opprettLøpendeFagsak()
    }

    private fun opprettLøpendeFagsak(): Fagsak {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())
        return fagsakService.lagre(fagsak.copy(status = FagsakStatus.LØPENDE))
    }

    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.INCLUDE)
    fun `skal kunne sette en behandling med status UTREDES eller SATT_PÅ_VENT på maskinell vent`(status: BehandlingStatus) {
        val behandling = opprettBehandling(status = status)

        snikeIKøenService.settAktivBehandlingTilPåVent(behandling.id)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling.id)
        assertThat(behandling.status).isEqualTo(status)
        assertThat(behandling.aktiv).isTrue()
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
        assertThat(oppdatertBehandling.aktiv).isFalse()
    }

    @Test
    fun `reaktivering av behandling skal sette status tilbake til UTREDES`() {
        val behandling1 = opprettBehandling()
        snikeIKøenService.settAktivBehandlingTilPåVent(behandling1.id)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

        snikeIKøenService.reaktiverBehandlingPåVent(behandling1.fagsak.id, behandling1.id, behandling2.id)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `reaktivering av behandling som er på vent skal sette status tilbake til SATT_PÅ_VENT`() {
        val behandling1 = opprettBehandling()
        settPåVentService.settBehandlingPåVent(behandling1.id, LocalDate.now(), SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
        snikeIKøenService.settAktivBehandlingTilPåVent(behandling1.id)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

        snikeIKøenService.reaktiverBehandlingPåVent(behandling1.fagsak.id, behandling1.id, behandling2.id)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `siste behandlingen er den som er aktiv til at behandlingen som er satt på vent er aktivert på nytt`() {
        opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling2)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert, men ennå ikke iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåVent(fagsak.id, behandling1.id, behandling2.id)

        validerSisteBehandling(behandling2)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert og iverksatt, og er då siste aktive behandlingen`() {
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåVent(fagsak.id, behandling1.id, behandling2.id)
        lagUtbetalingsoppdragOgAvslutt(behandling1)

        validerSisteBehandling(behandling1)
        validerErAktivBehandling(behandling1)
    }

    @Nested
    inner class ValideringAvSettPåVent {

        @Test
        fun `kan ikke sette en inaktiv behandling på vent`() {
            val behandling = opprettBehandling(aktiv = false)

            assertThatThrownBy {
                snikeIKøenService.settAktivBehandlingTilPåVent(behandling.id)
            }.hasMessageContaining("er ikke aktiv")
        }

        @ParameterizedTest
        @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.EXCLUDE)
        fun `kan ikke sette en behandling på vent med annen status enn UTREDES eller SATT_PÅ_VENT`(status: BehandlingStatus) {
            val behandling = opprettBehandling(status = status)

            assertThatThrownBy {
                snikeIKøenService.settAktivBehandlingTilPåVent(behandling.id)
            }.hasMessageContaining("kan ikke settes på maskinell vent då status")
        }
    }

    @Nested
    inner class ValideringAvReaktiverBehandling {

        @Test
        fun `skal feile når åpen behandling er aktiv`() {
            val behandlingPåVent = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = true)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)

            assertThatThrownBy { reaktiverBehandling(behandlingPåVent, behandlingSomSnekIKøen) }
                .hasMessageContaining("Åpen behandling har feil tilstand")
        }

        @Test
        fun `skal feile når åpen behandling ikke har status SATT_PÅ_MASKINELL_VENT`() {
            BehandlingStatus.values().filter { it != BehandlingStatus.SATT_PÅ_MASKINELL_VENT }.forEach {
                behandlingRepository.deleteAll()
                val behandlingPåVent = opprettBehandling(status = it, aktiv = false)
                val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true)

                assertThatThrownBy { reaktiverBehandling(behandlingPåVent, behandlingSomSnekIKøen) }
                    .hasMessageContaining("Åpen behandling har feil tilstand")
            }
        }

        @Test
        fun `skal feile når behandling som snek i køen ikke er aktiv`() {
            val behandlingPåVent = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false)

            assertThatThrownBy { reaktiverBehandling(behandlingPåVent, behandlingSomSnekIKøen) }
                .hasMessageContaining("som snek i køen må være aktiv")
        }

        @Test
        fun `skal feile når behandling som snek i køen har status satt på vent`() {
            val behandlingPåVent = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true)

            assertThatThrownBy { reaktiverBehandling(behandlingPåVent, behandlingSomSnekIKøen) }
                .hasMessageContaining("er ikke avsluttet")
        }
    }

    private fun reaktiverBehandling(
        behandlingPåVent: Behandling,
        behandlingSomSnekIKøen: Behandling,
    ) {
        snikeIKøenService.reaktiverBehandlingPåVent(
            fagsak.id,
            behandlingPåVent.id,
            behandlingSomSnekIKøen.id,
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
            behandlingRepository.saveAndFlush(behandling)
        }
    }

    private fun opprettBehandling(
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        aktiv: Boolean = true,
    ): Behandling = opprettBehandling(fagsak, status, aktiv)

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus,
        aktiv: Boolean,
    ): Behandling {
        if (skalVenteLitt) {
            Thread.sleep(10)
        } else {
            skalVenteLitt = true
        }
        val behandling = Behandling(
            fagsak = fagsak,
            opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            type = BehandlingType.REVURDERING,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            status = status,
            aktiv = aktiv,
        ).initBehandlingStegTilstand()
        return behandlingRepository.saveAndFlush(behandling)
    }
}
