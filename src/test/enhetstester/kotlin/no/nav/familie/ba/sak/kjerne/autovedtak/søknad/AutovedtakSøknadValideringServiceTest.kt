package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagInfotrygdstønad
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.assertj.core.api.Assertions.assertThat
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
    private val infotrygdService = mockk<InfotrygdService>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()

    private val autovedtakSøknadValideringService =
        AutovedtakSøknadValideringService(
            vilkårsvurderingService = vilkårsvurderingService,
            beregningService = beregningService,
            søknadGrunnlagService = søknadGrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            infotrygdService = infotrygdService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
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
    inner class ValiderAtVilkårsvurderingErOppfylt {
        @Test
        fun `skal ikke kaste feil når vilkårsvurderingen er oppfylt`() {
            // Arrange
            every { vilkårsvurderingService.hentAktivForBehandlingThrows(behandling.id) } returns lagVilkårsvurdering(behandling = behandling)

            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtVilkårsvurderingErOppfylt(behandling) }
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
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtVilkårsvurderingErOppfylt(behandling) }
        }
    }

    @Nested
    inner class ValiderAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger {
        @BeforeEach
        fun setup() {
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12), aktør = barnFremstiltKravFor, behandling = behandling))
            every { infotrygdService.hentInfotrygdstønaderForSøker(behandling.fagsak.aktør.aktivFødselsnummer(), historikk = true) } returns
                InfotrygdSøkResponse(bruker = emptyList(), barn = emptyList())
        }

        @Test
        fun `skal ikke kaste feil når det ikke finnes forrige vedtatte behandling`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger(behandling) }
        }

        @Test
        fun `skal kaste feil når innvilget periode overlapper med utbetaling i forrige vedtatte behandling`() {
            // Arrange
            val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, resultat = Behandlingsresultat.INNVILGET)
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns
                listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2024, 1), tom = YearMonth.of(2025, 6), aktør = barnUtenKrav, behandling = forrigeBehandling))

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger(behandling) }
        }

        @Test
        fun `skal kaste feil når innvilget periode overlapper med stønad til søker i Infotrygd`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every { infotrygdService.hentInfotrygdstønaderForSøker(behandling.fagsak.aktør.aktivFødselsnummer(), historikk = true) } returns
                InfotrygdSøkResponse(bruker = listOf(lagInfotrygdstønad(virkningFom = YearMonth.of(2024, 1), opphørtFom = YearMonth.of(2025, 7))), barn = emptyList())

            // Act
            val feil = assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtInnvilgedePerioderIkkeOverlapperMedTidligereUtbetalinger(behandling) }

            // Assert
            assertThat(feil.message).isEqualTo(
                "Automatisk behandling av søknad innvilger for periode(r) som overlapper med tidligere utbetaling til søker i Infotrygd.\nBehandling av søknad må håndteres manuelt.",
            )
        }
    }

    @Nested
    inner class ValiderAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget {
        @Test
        fun `skal ikke kaste feil når behandlingsresultatet er innvilget`() {
            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandling) }
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

            // Act & Assert
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtBehandlingsresultatErInnvilgetEllerDelvisInnvilget(behandlingMedAvslag) }
        }
    }

    @Nested
    inner class ValiderAtKunPersonerFremstiltKravForHarEndringIAndeler {
        @BeforeEach
        fun setup() {
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 1), tom = YearMonth.of(2030, 12), aktør = barnFremstiltKravFor, behandling = behandling))
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null
            every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = null) } returns listOf(barnFremstiltKravFor)
        }

        @Test
        fun `skal ikke kaste feil når kun personer det er fremstilt krav for har endring i andeler`() {
            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(behandling) }
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
            assertThrows<AutovedtakMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(behandling) }
        }

        @Test
        fun `skal hente personer fremstilt krav for med forrige vedtatte behandling`() {
            // Arrange
            val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD, resultat = Behandlingsresultat.INNVILGET)
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
            every { beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns emptyList()
            every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling) } returns listOf(barnFremstiltKravFor)

            // Act
            autovedtakSøknadValideringService.validerAtKunPersonerFremstiltKravForHarEndringIAndeler(behandling)

            // Assert
            verify(exactly = 1) {
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
                søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling)
            }
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

    @Nested
    inner class ValiderAtIngenUtbetalingerOverstiger100Prosent {
        @BeforeEach
        fun setup() {
            justRun { tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling) }
            justRun { tilkjentYtelseValideringService.validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(behandling) }
        }

        @Test
        fun `skal ikke kaste feil når ingen utbetalinger overstiger 100 prosent`() {
            // Act & Assert
            assertDoesNotThrow { autovedtakSøknadValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling) }
        }

        @Test
        fun `skal kaste feil med årsak for barn når barn får mer enn 100 prosent utbetaling`() {
            // Arrange
            every { tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling) } throws UtbetalingsikkerhetFeil("Over 100% for barn født 01.01.20")

            // Act
            val feil = assertThrows<AutovedtakSøknadMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling) }

            // Assert
            assertThat(feil.årsaker).containsExactly(AutovedtakSøknadMåBehandlesManueltÅrsak.UTBETALING_OVER_100_PROSENT_BARN)
            assertThat(feil.beskrivelse).doesNotContain("01.01.20")
            verify(exactly = 0) { tilkjentYtelseValideringService.validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(any()) }
        }

        @Test
        fun `skal kaste feil med årsak for søker når søker får mer enn 100 prosent utvidet barnetrygd`() {
            // Arrange
            every { tilkjentYtelseValideringService.validerAtSøkerIkkeFårFlereUtvidetAndelerSammePeriode(behandling) } throws UtbetalingsikkerhetFeil("Over 100% utvidet")

            // Act
            val feil = assertThrows<AutovedtakSøknadMåBehandlesManueltFeil> { autovedtakSøknadValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling) }

            // Assert
            assertThat(feil.årsaker).containsExactly(AutovedtakSøknadMåBehandlesManueltÅrsak.UTBETALING_OVER_100_PROSENT_SØKER)
        }
    }
}
