package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgValiderteEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingsresultatStegTest {

    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    private val behandlingService: BehandlingService = mockk()

    private val simuleringService: SimuleringService = mockk()

    private val vedtakService: VedtakService = mockk()

    private val vedtaksperiodeService: VedtaksperiodeService = mockk()

    private val mockBehandlingsresultatService: BehandlingsresultatService = mockk()

    private val vilkårService: VilkårService = mockk()

    private val persongrunnlagService: PersongrunnlagService = mockk()

    private val beregningService: BeregningService = mockk()

    private lateinit var behandlingsresultatSteg: BehandlingsresultatSteg

    private lateinit var behandling: Behandling

    private val featureToggleService: FeatureToggleService = mockk()

    private val andelerTilkjentYtelseOgValiderteEndreteUtbetalingerService =
        mockk<AndelerTilkjentYtelseOgValiderteEndreteUtbetalingerService>()

    @BeforeEach
    fun init() {
        behandlingsresultatSteg = BehandlingsresultatSteg(
            behandlingHentOgPersisterService,
            behandlingService,
            simuleringService,
            vedtakService,
            vedtaksperiodeService,
            mockBehandlingsresultatService,
            vilkårService,
            persongrunnlagService,
            beregningService,
            featureToggleService,
            andelerTilkjentYtelseOgValiderteEndreteUtbetalingerService
        )

        behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any()
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Ta kontakt med Team familie om du er uenig i resultatet.",
            exception.message
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Delvis Innvilget for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.DELVIS_INNVILGET

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any()
            )
        } returns behandling.copy(resultat = Behandlingsresultat.DELVIS_INNVILGET)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Delvis innvilget. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Ta kontakt med Team familie om du er uenig i resultatet.",
            exception.message
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått,Endret og Opphørt for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any()
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått, endret og opphørt. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Ta kontakt med Team familie om du er uenig i resultatet.",
            exception.message
        )
    }
}
