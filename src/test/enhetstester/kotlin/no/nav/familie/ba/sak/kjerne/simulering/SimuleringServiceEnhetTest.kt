package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØknomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
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
    private val økonomiService: ØkonomiService = mockk()
    private val utbetalingsoppdragService: UtbetalingsoppdragService = mockk()
    private val beregningService: BeregningService = mockk()
    private val øknomiSimuleringMottakerRepository: ØknomiSimuleringMottakerRepository = mockk()
    private val tilgangService: TilgangService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val vedtakRepository: VedtakRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()

    private val simuleringService: SimuleringService = SimuleringService(
        økonomiKlient,
        økonomiService,
        utbetalingsoppdragService,
        beregningService,
        øknomiSimuleringMottakerRepository,
        tilgangService,
        featureToggleService,
        vedtakRepository,
        behandlingHentOgPersisterService,
        persongrunnlagService
    )

    val februar2023 = LocalDate.of(2023, 2, 1)

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av etterbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )

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

        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentBarna(behandling.id) } returns listOf(lagPerson(), lagPerson())
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere true dersom det finnes avvik i form av feilutbetaling som er innenfor beløpsgrense`(
        behandlingÅrsak: BehandlingÅrsak
    ) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )
        every { featureToggleService.isEnabled(FeatureToggleConfig.IKKE_STOPP_MIGRERINGSBEHANDLING) } returns false
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(4)

        val fom = LocalDate.of(2021, 1, 1)
        val tom = LocalDate.of(2021, 1, 31)
        val fom2 = LocalDate.of(2021, 2, 1)
        val tom2 = LocalDate.of(2021, 2, 28)

        // feilutbetaling 1 KR per barn i hver periode
        val posteringer = listOf(
            mockVedtakSimuleringPostering(fom = fom, tom = tom, beløp = 2, posteringType = PosteringType.FEILUTBETALING),
            mockVedtakSimuleringPostering(fom = fom2, tom = tom2, beløp = 2, posteringType = PosteringType.FEILUTBETALING)
        )

        val simuleringMottaker =
            listOf(mockØkonomiSimuleringMottaker(behandling = behandling, økonomiSimuleringPostering = posteringer))

        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentBarna(behandling.id) } returns listOf(lagPerson(), lagPerson())
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(true))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal returnere false dersom det finnes avvik i form av feilutbetaling som er utenfor beløpsgrense`(behandlingÅrsak: BehandlingÅrsak) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
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

        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns simuleringMottaker
        every { persongrunnlagService.hentBarna(behandling.id) } returns listOf(lagPerson(), lagPerson())
        every { featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ) } returns true

        val behandlingHarAvvikInnenforBeløpsgrenser =
            simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling)

        assertThat(behandlingHarAvvikInnenforBeløpsgrenser, Is(false))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["HELMANUELL_MIGRERING", "ENDRE_MIGRERINGSDATO"])
    fun `harMigreringsbehandlingAvvikInnenforBeløpsgrenser skal kaste feil dersom behandlingen ikke er en manuell migrering`(behandlingÅrsak: BehandlingÅrsak) {
        val behandling: Behandling = no.nav.familie.ba.sak.common.lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = behandlingÅrsak,
            førsteSteg = StegType.VURDER_TILBAKEKREVING
        )

        assertThrows<Feil> { simuleringService.harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling) }
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
