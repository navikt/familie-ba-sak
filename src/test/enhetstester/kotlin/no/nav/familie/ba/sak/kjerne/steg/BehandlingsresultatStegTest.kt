package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.ENDRE_MIGRERINGSDATO
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FINNMARKSTILLEGG
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.MÅNEDLIG_VALUTAJUSTERING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SATSENDRING
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SVALBARDTILLEGG
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE

class BehandlingsresultatStegTest {
    private val behandlingService: BehandlingService = mockk()
    private val vedtakService: VedtakService = mockk()
    private val vedtaksperiodeService: VedtaksperiodeService = mockk()
    private val behandlingsresultatService: BehandlingsresultatService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val beregningService: BeregningService = mockk()
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()
    private val behandlingsresultatstegValideringService: BehandlingsresultatStegValideringService = mockk()

    private val behandlingsresultatSteg: BehandlingsresultatSteg =
        BehandlingsresultatSteg(
            behandlingService = behandlingService,
            simuleringService = mockk(),
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingsresultatService = behandlingsresultatService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            småbarnstilleggService = mockk(),
            tilbakestillBehandlingService = tilbakestillBehandlingService,
            behandlingsresultatstegValideringService = behandlingsresultatstegValideringService,
        )

    @Nested
    inner class UtførStegOgAngiNesteTest {
        @BeforeEach
        fun init() {
            justRun { tilbakestillBehandlingService.slettTilbakekrevingsvedtakMotregningHvisBehandlingIkkeAvregner(any()) }
        }

        @Test
        fun `Skal gå rett fra behandlingsresultat til iverksetting for alle fødselshendelser`() {
            // Arrange
            val fødselshendelseBehandling =
                lagBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    skalBehandlesAutomatisk = true,
                )

            every { behandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.INNVILGET_OG_ENDRET
            every { behandlingService.nullstillEndringstidspunkt(fødselshendelseBehandling.id) } just runs
            every { behandlingService.oppdaterBehandlingsresultat(any(), any(), any()) } returns
                fødselshendelseBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
            every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns
                fødselshendelseBehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)
            every { vedtakService.hentAktivForBehandlingThrows(fødselshendelseBehandling.id) } returns lagVedtak(fødselshendelseBehandling)
            every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(any()) } just runs
            every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(fødselshendelseBehandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

            // Act
            val nesteSteg = behandlingsresultatSteg.utførStegOgAngiNeste(fødselshendelseBehandling, "")

            // Assert
            assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
        }
    }

    @Nested
    inner class PreValiderStegTest {
        @BeforeEach
        fun setup() {
            every { persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(any()) } returns listOf(lagPersonEnkel(personType = PersonType.SØKER))
            every { beregningService.hentTilkjentYtelseForBehandling(any()) } returns mockk(relaxed = true)

            justRun { behandlingsresultatstegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(any()) }
            justRun { behandlingsresultatstegValideringService.validerSatsendring(any()) }
            justRun { behandlingsresultatstegValideringService.validerFinnmarkstilleggBehandling(any()) }
            justRun { behandlingsresultatstegValideringService.validerSvalbardtilleggBehandling(any()) }
            justRun { behandlingsresultatstegValideringService.validerEndredeUtbetalingsandeler(any()) }
            justRun { behandlingsresultatstegValideringService.validerKompetanse(any()) }
            justRun { behandlingsresultatstegValideringService.validerAtDetIkkeFinnesPerioderMedSekundærlandKompetanseUtenUtenlandskbeløpEllerValutakurs(any()) }
            justRun { behandlingsresultatstegValideringService.validerIngenEndringTilbakeITid(any()) }
            justRun { behandlingsresultatstegValideringService.validerSatsErUendret(any()) }
            justRun { behandlingsresultatstegValideringService.validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(any()) }
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"],
            mode = EXCLUDE,
        )
        fun `skal ikke valideres om behandlingen ikke har riktig årsak for behandling som skal automatisk behandles`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(skalBehandlesAutomatisk = true, årsak = behandlingÅrsak)

            // Act & assert
            assertDoesNotThrow { behandlingsresultatSteg.preValiderSteg(behandling) }
        }

        @Test
        fun `skal validere at utenlandsk periodebeløp og valutakurs er utfylt`() {
            // Arrange
            val behandling = lagBehandling()

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerAtUtenlandskPeriodebeløpOgValutakursErUtfylt(behandling)
            }
        }

        @Test
        fun `skal validere satsendring`() {
            // Arrange
            val behandling = lagBehandling(årsak = SATSENDRING)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerSatsendring(any())
            }
        }

        @Test
        fun `skal validere finnmarkstillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = FINNMARKSTILLEGG)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerFinnmarkstilleggBehandling(any())
            }
        }

        @Test
        fun `skal validere svalbardtillegg`() {
            // Arrange
            val behandling = lagBehandling(årsak = SVALBARDTILLEGG)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerSvalbardtilleggBehandling(any())
            }
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"],
            mode = EXCLUDE,
        )
        fun `skal validere endrede utbetalinger og kompetanse`(
            behandlingsÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(årsak = behandlingsÅrsak)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerEndredeUtbetalingsandeler(any())
                behandlingsresultatstegValideringService.validerKompetanse(any())
                behandlingsresultatstegValideringService.validerAtDetIkkeFinnesPerioderMedSekundærlandKompetanseUtenUtenlandskbeløpEllerValutakurs(any())
            }
        }

        @Test
        fun `skal validere månedlig valutajustering`() {
            // Arrange
            val behandling = lagBehandling(årsak = MÅNEDLIG_VALUTAJUSTERING)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerIngenEndringTilbakeITid(any())
                behandlingsresultatstegValideringService.validerSatsErUendret(any())
            }
        }

        @Test
        fun `skal validere endre migreringsdato`() {
            // Arrange
            val behandling = lagBehandling(årsak = ENDRE_MIGRERINGSDATO)

            // Act
            behandlingsresultatSteg.preValiderSteg(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingsresultatstegValideringService.validerIngenEndringIUtbetalingEtterMigreringsdatoenTilForrigeIverksatteBehandling(any())
            }
        }
    }
}
