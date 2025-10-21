package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkel
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class SimuleringServiceEnhetTest {
    private val økonomiKlient: ØkonomiKlient = mockk()
    private val beregningService: BeregningService = mockk()
    private val økonomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository = mockk()
    private val tilgangService: TilgangService = mockk()
    private val vedtakRepository: VedtakRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator = mockk()
    private val tilkjentYtelseRepository: TilkjentYtelseRepository = mockk()

    private val simuleringService: SimuleringService =
        SimuleringService(
            økonomiKlient = økonomiKlient,
            beregningService = beregningService,
            økonomiSimuleringMottakerRepository = økonomiSimuleringMottakerRepository,
            tilgangService = tilgangService,
            vedtakRepository = vedtakRepository,
            utbetalingsoppdragGenerator = utbetalingsoppdragGenerator,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
        )

    val februar2023 = LocalDate.of(2023, 2, 1)

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av etterbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )

        // etterbetaling 4 KR pga. avrundingsfeil. 1 KR per barn i hver periode.
        val posteringer =
            listOf(
                mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(fom = februar2023, beløp = -2, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(fom = februar2023, beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = -2, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(beløp = 2, betalingType = BetalingType.DEBIT),
            )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns
            listOf(
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            )

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av feilutbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(4)

        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        val fom2 = LocalDate.of(2021, 2, 1)
        val tom2 = LocalDate.of(2021, 2, 28)

        // feilutbetaling 1 KR per barn i hver periode
        val posteringer =
            listOf(
                mockVedtakSimuleringPostering(
                    fom = fom,
                    tom = tom,
                    beløp = 2,
                    posteringType = PosteringType.FEILUTBETALING,
                ),
                mockVedtakSimuleringPostering(
                    fom = fom2,
                    tom = tom2,
                    beløp = 2,
                    posteringType = PosteringType.FEILUTBETALING,
                ),
            )

        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns
            listOf(
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            )

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere false dersom det finnes avvik i form av feilutbetaling som er utenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id) } returns
            listOf(
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
                lagPerson(type = PersonType.BARN).tilPersonEnkel(),
            )

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(false))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal kaste feil dersom behandlingen ikke er en manuell migrering`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )

        assertThrows<Feil> { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal returnere true dersom det finnes manuelle posteringer i simuleringsresultat før mars 2023`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(
                    beløp = 200,
                    betalingType = BetalingType.DEBIT,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
            )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker

        val behandlingHarManuellePosteringerFørMars2023 =
            simuleringService.harMigreringsbehandlingManuellePosteringer(behandling)

        assertThat(behandlingHarManuellePosteringerFørMars2023, Is(true))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal returnere false dersom det ikke finnes manuelle posteringer i simuleringsresultat før mars 2023`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO

        // etterbetaling 200 KR
        val posteringer =
            listOf(
                mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
                mockVedtakSimuleringPostering(beløp = -200, betalingType = BetalingType.KREDIT),
                mockVedtakSimuleringPostering(beløp = 200, betalingType = BetalingType.DEBIT),
            )
        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { økonomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker

        val behandlingHarManuellePosteringerFørMars2023 =
            simuleringService.harMigreringsbehandlingManuellePosteringer(behandling)

        assertThat(behandlingHarManuellePosteringerFørMars2023, Is(false))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"],
    )
    fun `harMigreringsbehandlingManuellePosteringerFørMars2023 skal kaste feil dersom behandlingen ikke er en manuell migrering`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling: Behandling =
            lagBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = behandlingÅrsak,
                førsteSteg = StegType.VURDER_TILBAKEKREVING,
            )

        assertThrows<Feil> { simuleringService.harMigreringsbehandlingManuellePosteringer(behandling) }
    }

    private fun mockØkonomiSimuleringMottaker(
        id: Long = 0,
        mottakerNummer: String? = randomFnr(),
        mottakerType: MottakerType = MottakerType.BRUKER,
        behandling: Behandling = mockk(relaxed = true),
        økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockVedtakSimuleringPostering()),
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
        utenInntrekk: Boolean = false,
        fagsakId: Long = 0L,
    ) = ØkonomiSimuleringPostering(
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
        fagOmrådeKode = fagOmrådeKode,
        fom = fom,
        tom = tom,
        betalingType = betalingType,
        beløp = beløp.toBigDecimal(),
        posteringType = posteringType,
        forfallsdato = forfallsdato,
        utenInntrekk = utenInntrekk,
        fagsakId = fagsakId,
    )
}
