package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class AutovedtakSøknadValideringServiceTest {
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val beregningService = mockk<BeregningService>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    private val autovedtakSøknadValideringService =
        AutovedtakSøknadValideringService(
            vilkårsvurderingService = vilkårsvurderingService,
            beregningService = beregningService,
            søknadGrunnlagService = søknadGrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    private val behandling =
        lagBehandling(
            årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
            skalBehandlesAutomatisk = true,
            resultat = Behandlingsresultat.INNVILGET,
        )
    private val barnFremstiltKravFor = lagPerson(type = PersonType.BARN).aktør
    private val barnUtenKrav = lagPerson(type = PersonType.BARN).aktør

    @Nested
    inner class ValiderAtBehandlingKanVedtasAutomatisk {
        @BeforeEach
        fun setup() {
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns lagVilkårsvurdering(behandling = behandling)
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12), aktør = barnFremstiltKravFor, behandling = behandling))
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null) } returns listOf(barnFremstiltKravFor)
        }

        @Test
        fun `skal ikke kaste feil når behandlingen kan vedtas automatisk`() {
            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling) }
        }

        @Test
        fun `skal kaste feil når vilkårsvurderingen ikke er oppfylt`() {
            // Arrange
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = barnFremstiltKravFor,
                                lagVilkårResultater = { personResultat ->
                                    setOf(lagVilkårResultat(personResultat = personResultat, resultat = Resultat.IKKE_OPPFYLT))
                                },
                            ),
                        )
                    },
                )

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling) }
        }

        @Test
        fun `skal kaste feil når behandlingsresultatet ikke er innvilget`() {
            // Arrange
            val behandlingMedAvslag =
                lagBehandling(
                    årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                    skalBehandlesAutomatisk = true,
                    resultat = Behandlingsresultat.AVSLÅTT,
                )
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingMedAvslag.id) } returns lagVilkårsvurdering(behandling = behandlingMedAvslag)

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandlingMedAvslag) }
        }

        @Test
        fun `skal kaste feil når barn uten krav har endring i andeler`() {
            // Arrange
            val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, resultat = Behandlingsresultat.INNVILGET)
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12), aktør = barnUtenKrav))
            every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling) } returns listOf(barnFremstiltKravFor)

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling) }
        }

        @Test
        fun `skal hente personer fremstilt krav for med forrige vedtatte behandling`() {
            // Arrange
            val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, resultat = Behandlingsresultat.INNVILGET)
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns emptyList()
            every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling) } returns listOf(barnFremstiltKravFor)

            // Act
            autovedtakSøknadValideringService.validerAtBehandlingKanVedtasAutomatisk(behandling)

            // Assert
            verify(exactly = 1) { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling) }
        }
    }

    @Nested
    inner class ValiderAtSimuleringGirUtbetalingUtenFeilutbetaling {
        @Test
        fun `skal ikke kaste feil når simuleringen gir utbetaling uten feilutbetaling`() {
            // Arrange
            val simulering =
                listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100))))

            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når simuleringen gir feilutbetaling`() {
            // Arrange
            val simulering =
                listOf(
                    lagØkonomiSimuleringMottaker(
                        behandling = behandling,
                        økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.FEILUTBETALING)),
                    ),
                )

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simulering) }
        }

        @Test
        fun `skal kaste feil når simuleringen ikke gir utbetaling`() {
            // Arrange
            val simuleringUtenUtbetaling =
                listOf(lagØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = listOf(lagØkonomiSimuleringPostering(beløp = 0))))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtSimuleringGirUtbetalingUtenFeilutbetaling(simuleringUtenUtbetaling) }
        }
    }
}
