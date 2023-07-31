package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

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

    private val andelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()

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
            andelerTilkjentYtelseOgEndreteUtbetalingerService,
            andelTilkjentYtelseRepository,
        )

        behandling = lagBehandling(
            behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
            årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Delvis Innvilget for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.DELVIS_INNVILGET

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.DELVIS_INNVILGET)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Delvis innvilget. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste exception hvis behandlingsresultat er Avslått,Endret og Opphørt for en manuell migrering`() {
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT

        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns behandling.copy(resultat = Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT)

        val exception = assertThrows<RuntimeException> { behandlingsresultatSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Du har fått behandlingsresultatet Avslått, endret og opphørt. " +
                "Dette er ikke støttet på migreringsbehandlinger. " +
                "Meld sak i Porten om du er uenig i resultatet.",
            exception.message,
        )
    }

    @Test
    fun `skal kaste feil om det er endring etter migreringsdatoen til første behandling`() {
        val startdato = YearMonth.of(2023, 2)
        val endringTidslinje = "TTTFFFF".tilBoolskTidslinje(
            startdato,
        )

        assertThrows<FunksjonellFeil> {
            endringTidslinje.kastFeilVedEndringEtter(startdato, lagBehandling())
        }
    }

    @Test
    fun `skal ikke kaste feil om det ikke er endring etter migreringsdatoen til første behandling`() {
        val startdato = YearMonth.of(2023, 2)
        val treMånederEtterStartdato = startdato.plusMonths(3)

        val endringTidslinje = "TTTFFFF".tilBoolskTidslinje(
            startdato,
        )

        assertDoesNotThrow {
            endringTidslinje.kastFeilVedEndringEtter(treMånederEtterStartdato, lagBehandling())
        }
    }

    @Test
    fun `skal gå rett fra behandlingsresultat til iverksetting for alle fødselshendelser`() {
        val fødselshendelseBehandling = behandling.copy(
            skalBehandlesAutomatisk = true,
            opprettetÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
        )
        val vedtak = lagVedtak(
            fødselshendelseBehandling,
        )
        every { mockBehandlingsresultatService.utledBehandlingsresultat(any()) } returns Behandlingsresultat.INNVILGET_OG_ENDRET
        every { behandlingService.nullstillEndringstidspunkt(fødselshendelseBehandling.id) } just runs
        every {
            behandlingService.oppdaterBehandlingsresultat(
                any(),
                any(),
            )
        } returns fødselshendelseBehandling.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)
        every {
            behandlingService.oppdaterStatusPåBehandling(
                fødselshendelseBehandling.id,
                BehandlingStatus.IVERKSETTER_VEDTAK,
            )
        } returns fødselshendelseBehandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)
        every { vedtakService.hentAktivForBehandlingThrows(fødselshendelseBehandling.id) } returns vedtak
        every { vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(vedtak) } just runs
        every { beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(fødselshendelseBehandling) } returns EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

        assertEquals(
            behandlingsresultatSteg.utførStegOgAngiNeste(fødselshendelseBehandling, ""),
            StegType.IVERKSETT_MOT_OPPDRAG,
        )
    }

    fun String.tilBoolskTidslinje(startdato: YearMonth): Tidslinje<Boolean, Måned> {
        return tidslinje {
            this.mapIndexed { index, it ->
                Periode(
                    startdato.plusMonths(index.toLong()).tilTidspunkt(),
                    startdato.plusMonths(index.toLong()).tilTidspunkt(),
                    when (it) {
                        'T' -> true
                        'F' -> false
                        else -> throw Feil("Klarer ikke å konvertere \"$it\" til Boolean")
                    },
                )
            }
        }
    }
}
