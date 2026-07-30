package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class BrevmalServiceTest {
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val brevmalService = BrevmalService(andelTilkjentYtelseRepository, featureToggleService)

    @Test
    fun `hentBrevmal skal returnere VEDTAK_OPPHØR_DØDSFALL dersom behandlingårsak er DØDSFALL_BRUKER`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)

        assertThat(brevmalService.hentBrevmal(behandling), Is(Brevmal.VEDTAK_OPPHØR_DØDSFALL))
    }

    @Test
    fun `hentVedtaksbrevmal skal kaste feil dersom behandling har status IKKE_VURDERT`() {
        val behandling =
            lagBehandling(årsak = BehandlingÅrsak.KORREKSJON_VEDTAKSBREV, resultat = Behandlingsresultat.IKKE_VURDERT)

        assertThrows<FunksjonellFeil> {
            brevmalService.hentVedtaksbrevmal(behandling)
        }
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON for førstegangsbehandling som er institusjon med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON for førstegangsbehandling som er institusjon med gitte typer behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>().apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns mockk()
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON))
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_FØRSTEGANGSVEDTAK for førstegangsbehandling med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_FØRSTEGANGSVEDTAK for førstegangsbehandling med gitte behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>().apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns null
                every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_FØRSTEGANGSVEDTAK))
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON for førstegangsbehandling som er institusjon med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON for revurdering med ingen løpende ytelser som er institusjon med gitte behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>(relaxed = true).apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns mockk()
                every { type } returns BehandlingType.REVURDERING
            }

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(1999, 1),
                    tom = YearMonth.of(1999, 2),
                ),
            )

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON))
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_ENDRING_INSTITUSJON for førstegangsbehandling som er institusjon med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_ENDRING_INSTITUSJON for revurdering med løpende ytelser som er institusjon med gitte typer behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>(relaxed = true).apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns mockk()
                every { type } returns BehandlingType.REVURDERING
            }

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 1),
                    tom = YearMonth.of(2037, 2),
                ),
            )

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_ENDRING_INSTITUSJON))
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_ENDRING for førstegangsbehandling som er institusjon med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_ENDRING for revurdering med løpende ytelser med gitte behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>(relaxed = true).apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns null
                every { type } returns BehandlingType.REVURDERING
            }

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 1),
                    tom = YearMonth.of(2037, 2),
                ),
            )

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_ENDRING))
    }

    @ParameterizedTest(name = "hentManuellVedtaksbrevtype skal returnere VEDTAK_OPPHØR_MED_ENDRING for førstegangsbehandling som er institusjon med behandlingsresultat {0}")
    @EnumSource(
        value = Behandlingsresultat::class,
        names = ["INNVILGET", "INNVILGET_OG_ENDRET", "INNVILGET_OG_OPPHØRT", "INNVILGET_ENDRET_OG_OPPHØRT", "DELVIS_INNVILGET", "DELVIS_INNVILGET_OG_ENDRET", "DELVIS_INNVILGET_OG_OPPHØRT", "DELVIS_INNVILGET_ENDRET_OG_OPPHØRT", "AVSLÅTT_OG_ENDRET", "AVSLÅTT_OG_OPPHØRT", "AVSLÅTT_ENDRET_OG_OPPHØRT"],
    )
    fun `hentManuellVedtaksbrevtype skal returnere VEDTAK_OPPHØR_MED_ENDRING for revurdering med ingen løpende ytelser med gitte behandlingsresultat `(behandlingsresultat: Behandlingsresultat) {
        val behandling =
            mockk<Behandling>(relaxed = true).apply {
                every { resultat } returns behandlingsresultat
                every { fagsak.institusjon } returns null
                every { type } returns BehandlingType.REVURDERING
            }

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(1999, 1),
                    tom = YearMonth.of(1999, 2),
                ),
            )

        assertThat(brevmalService.hentManuellVedtaksbrevtype(behandling), Is(Brevmal.VEDTAK_OPPHØR_MED_ENDRING))
    }

    @Test
    fun `hentVedtaksbrevmal skal returnere AUTOVEDTAK_ENDRING for automatisk skjermet barn behandling omregning 18 år`() {
        val skjermetBarnFagsak =
            lagFagsak(
                type = FagsakType.SKJERMET_BARN,
                status = FagsakStatus.LØPENDE,
                skjermetBarnSøker = SkjermetBarnSøker(aktør = randomAktør()),
            )
        val behandling =
            lagBehandling(
                fagsak = skjermetBarnFagsak,
                årsak = BehandlingÅrsak.OMREGNING_18ÅR,
                skalBehandlesAutomatisk = true,
                behandlingType = BehandlingType.REVURDERING,
                resultat = Behandlingsresultat.ENDRET_OG_OPPHØRT,
            )

        assertThat(brevmalService.hentVedtaksbrevmal(behandling), Is(Brevmal.AUTOVEDTAK_ENDRING))
    }

    @Test
    fun `hentVedtaksbrevmal skal returnere AUTOVEDTAK_SATSENDRING_EØS for automatisk behandling med årsak SATSENDRING_EØS`() {
        val behandling =
            lagBehandling(
                årsak = BehandlingÅrsak.SATSENDRING_EØS,
                skalBehandlesAutomatisk = true,
                behandlingType = BehandlingType.REVURDERING,
                resultat = Behandlingsresultat.ENDRET_UTBETALING,
            )

        assertThat(brevmalService.hentVedtaksbrevmal(behandling), Is(Brevmal.AUTOVEDTAK_SATSENDRING_EØS))
    }
}
