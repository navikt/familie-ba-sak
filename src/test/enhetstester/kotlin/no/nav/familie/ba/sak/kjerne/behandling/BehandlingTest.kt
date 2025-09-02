package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BehandlingTest {
    @Test
    fun `validerBehandling kaster feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk endring`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.TEKNISK_ENDRING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster feil hvis man prøver å opprette revurdering uten andre vedtatte behandlinger`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster ikke feil hvis man prøver å opprette revurdering med andre vedtatte behandlinger`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SØKNAD,
            )
        assertDoesNotThrow {
            behandling.validerBehandlingstype(
                sisteBehandlingSomErVedtatt =
                    lagBehandling(
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        årsak = BehandlingÅrsak.SØKNAD,
                    ),
            )
        }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan sende vedtaksbrev for ordinær førstegangsbehandling`() {
        val behandling = lagBehandling()
        assertTrue { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan sende vedtaksbrev for ordinær revurdering`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            )
        assertTrue { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["ENDRE_MIGRERINGSDATO", "HELMANUELL_MIGRERING", "MIGRERING"])
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for behandlingstype MIGRERING_FRA_INFOTRYGD`(
        årsak: BehandlingÅrsak,
    ) {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = årsak,
            )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for teknisk endring`() {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.TEKNISK_ENDRING,
            )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING"])
    fun `erBehandlingMedVedtaksbrevutsending kan ikke sende vedtaksbrev for revurdering med årsak satsendring eller månedlig valutajustering`(
        årsak: BehandlingÅrsak,
    ) {
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = årsak,
            )
        assertFalse { behandling.erBehandlingMedVedtaksbrevutsending() }
    }

    @Test
    fun `Skal svare med overstyrt dokumenttittel på alle behandlinger som er definert som omgjøringsårsaker`() {
        BehandlingÅrsak.entries.forEach {
            if (it.erOmregningsårsak()) {
                assertNotNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            } else {
                assertNull(it.hentOverstyrtDokumenttittelForOmregningsbehandling())
            }
        }
    }
}
