package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.steg.VurderTilbakekrevingSteg.Companion.februar2023
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class VurderTilbakekrevingStegTest {

    private val tilbakekrevingService: TilbakekrevingService = mockk()
    private val simuleringService: SimuleringService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val personGrunnlagService: PersongrunnlagService = mockk()

    private val vurderTilbakekrevingSteg: VurderTilbakekrevingSteg =
        VurderTilbakekrevingSteg(featureToggleService, tilbakekrevingService, simuleringService, personGrunnlagService)

    private val behandling: Behandling = lagBehandling(
        behandlingType = BehandlingType.REVURDERING,
        årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        førsteSteg = StegType.VURDER_TILBAKEKREVING
    )
    private val restTilbakekreving: RestTilbakekreving = RestTilbakekreving(
        valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
        varsel = "testverdi",
        begrunnelse = "testverdi"
    )

    @BeforeEach
    fun setup() {
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns false
        every { tilbakekrevingService.validerRestTilbakekreving(any(), any()) } returns Unit
        every { tilbakekrevingService.lagreTilbakekreving(any(), any()) } returns null
        every { featureToggleService.isEnabled(any()) } returns true
        every { featureToggleService.isEnabled(any(), true) } returns true
        every { personGrunnlagService.hentBarna(any<Long>()) } returns listOf(lagPerson())
    }

    @Test
    fun `skal utføre steg for vanlig behandling uten åpen tilbakekreving`() {
        val stegType = assertDoesNotThrow {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 1) { tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id) }
        verify(exactly = 1) { tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id) }
    }

    @Test
    fun `skal utføre steg for vanlig behandling med åpen tilbakekreving`() {
        every { tilbakekrevingService.søkerHarÅpenTilbakekreving(any()) } returns true
        val stegType = assertDoesNotThrow {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        verify(exactly = 0) { tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandling.id) }
        verify(exactly = 0) { tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandling.id) }
    }

    @Test
    fun `skal ikke utføre steg for migreringsbehandling med endre migreringsdato når det finnes feilutbetaling over beløpsgrense`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(2500)
        every { simuleringService.hentEtterbetaling(behandling.id) } returns BigDecimal.ZERO
        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns emptyList()

        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 2500, posteringType = PosteringType.FEILUTBETALING)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val exception = assertThrows<RuntimeException> {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertEquals(
            "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling",
            exception.message
        )
    }

    @Test
    @Disabled("TODO Fikses senere")
    fun `skal utføre steg for migreringsbehandling når avvik i form av etterbetaling er under beløpsgrense`() {
        listOf(BehandlingÅrsak.HELMANUELL_MIGRERING, BehandlingÅrsak.ENDRE_MIGRERINGSDATO).forEach {
            val behandling: Behandling = lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = it,
                førsteSteg = StegType.VURDER_TILBAKEKREVING
            )
            every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(0)
            every { personGrunnlagService.hentBarna(any<Long>()) } returns listOf(lagPerson(), lagPerson())

            // etterbetaling 4 KR pga. avrundingsfeil. 1 KR per barn i hver periode.
            val posteringer = listOf(
                mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(fom = februar2023, beløp = -2, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = -2, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT)
            )
            val simuleringMottaker =
                listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

            every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

            val stegType = assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    restTilbakekreving
                )
            }
            assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        }
    }

    @Test
    fun `skal ikke utføre steg for migreringsbehandling med endre migreringsdato når det finnes etterbetaling mer enn maks beløp før satsendring 2023`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 2000 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = -1000, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -1000, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 1000, betalingType = BetalingType.DEBIT)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val exception = assertThrows<RuntimeException> {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertEquals(
            "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling",
            exception.message
        )
    }

    @Test
    fun `skal ikke utføre steg for helmanuell migrering når det finnes feilutbetaling over beløpsgrense`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(2500)
        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns emptyList()

        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 2500, posteringType = PosteringType.FEILUTBETALING)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val exception = assertThrows<RuntimeException> {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertEquals(
            "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling",
            exception.message
        )
    }

    @Test
    fun `skal utføre steg for migreringsbehandling når avvik i form av feilutbetaling er under beløpsgrense`() {
        listOf(BehandlingÅrsak.HELMANUELL_MIGRERING, BehandlingÅrsak.ENDRE_MIGRERINGSDATO).forEach {
            val behandling: Behandling = lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = it,
                førsteSteg = StegType.VURDER_TILBAKEKREVING
            )
            every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
            every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(4)
            every { personGrunnlagService.hentBarna(any<Long>()) } returns listOf(lagPerson(), lagPerson())

            val fom = LocalDate.of(2021, 1, 1)
            val tom = LocalDate.of(2021, 1, 31)
            val fom2 = LocalDate.of(2021, 2, 1)
            val tom2 = LocalDate.of(2021, 2, 28)

            // feilutbetaling 1 KR per barn i hver periode
            val posteringer = listOf(
                mockVedtakSimuleringPostering(
                    fom = fom,
                    tom = tom,
                    beløp = 2,
                    posteringType = PosteringType.FEILUTBETALING
                ),
                mockVedtakSimuleringPostering(
                    fom = fom2,
                    tom = tom2,
                    beløp = 2,
                    posteringType = PosteringType.FEILUTBETALING
                )
            )
            val simuleringMottaker =
                listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

            every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

            val stegType = assertDoesNotThrow {
                vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                    behandling,
                    restTilbakekreving
                )
            }
            assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
        }
    }

    @Test
    fun `skal ikke utføre steg for helmanuell migrering når det finnes etterbetaling mer enn maks beløp før satsendring 2023`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 2000 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = -1000, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = februar2023, beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = 1000, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -1000, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 1000, betalingType = BetalingType.DEBIT)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val exception = assertThrows<RuntimeException> {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertEquals(
            "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling",
            exception.message
        )
    }

    @Test
    fun `skal ikke utføre steg for helmanuell migrering når det finnes etterbetaling med flere perioder`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        val fom2 = LocalDate.of(2021, 2, 1)
        val tom2 = LocalDate.of(2021, 2, 28)

        // etterbetaling 440 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = 300, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = -300, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = 300, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = 140, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = -140, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = 140, betalingType = BetalingType.DEBIT)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val exception = assertThrows<RuntimeException> {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertEquals(
            "Migreringsbehandling med årsak ${behandling.opprettetÅrsak.visningsnavn} kan ikke fortsette " +
                "når det finnes feilutbetaling/etterbetaling",
            exception.message
        )
    }

    @Test
    fun `skal utføre steg for helmanuell migrering når det finnes etterbetaling mindre enn maks beløp`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val stegType = assertDoesNotThrow {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
    }

    @Test
    fun `skal utføre steg for helmanuell migrering når det finnes etterbetaling med flere periode`() {
        val behandling: Behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        val fom2 = LocalDate.of(2021, 2, 1)
        val tom2 = LocalDate.of(2021, 2, 28)

        // etterbetaling 440 KR
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = 220, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = -220, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = 220, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = 220, betalingType = BetalingType.DEBIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = -220, betalingType = BetalingType.KREDIT),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = 220, betalingType = BetalingType.DEBIT)
        )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns simuleringMottaker

        val stegType = assertDoesNotThrow {
            vurderTilbakekrevingSteg.utførStegOgAngiNeste(
                behandling,
                restTilbakekreving
            )
        }
        assertTrue { stegType == StegType.SEND_TIL_BESLUTTER }
    }

    private fun mockØkonomiSimuleringMottaker(
        id: Long = 0,
        mottakerNummer: String? = randomFnr(),
        mottakerType: MottakerType = MottakerType.BRUKER,
        behandling: Behandling = mockk(relaxed = true),
        økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockVedtakSimuleringPostering())
    ) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)

    private fun mockVedtakSimuleringPostering(
        økonomiSimuleringMottaker: ØkonomiSimuleringMottaker = mockk(relaxed = true),
        beløp: Int = 0,
        fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.BARNETRYGD,
        fom: LocalDate = LocalDate.of(2023, 1, 1),
        tom: LocalDate = LocalDate.of(2023, 1, 1),
        betalingType: BetalingType = BetalingType.DEBIT,
        posteringType: PosteringType = PosteringType.YTELSE,
        forfallsdato: LocalDate = LocalDate.of(2023, 1, 1),
        utenInntrekk: Boolean = false
    ) = ØkonomiSimuleringPostering(
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
        fagOmrådeKode = fagOmrådeKode,
        fom = fom,
        tom = tom,
        betalingType = betalingType,
        beløp = beløp.toBigDecimal(),
        posteringType = posteringType,
        forfallsdato = forfallsdato,
        utenInntrekk = utenInntrekk
    )
}
