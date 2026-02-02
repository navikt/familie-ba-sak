package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring.Companion.SATSENDRINGMÅNED_FEB_2026
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlag
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AutovedtakSatsendringServiceTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val registrerPersongrunnlag: RegistrerPersongrunnlag,
    @Autowired private val satsendringService: SatsendringService,
) : AbstractSpringIntegrationTest() {
    private lateinit var fagsak: Fagsak
    private lateinit var aktørBarn: Aktør

    @BeforeEach
    fun setUp() {
        // Vilkårsvurdering og andeler tilkjent ytelse blir ikke generert i disse testene. Validering av andeler ved satsendring vil derfor kaste feil. For at testene for sett på vent skal fungere skrur vi her av denne valideringen.
        mockkObject(TilkjentYtelseValidering)
        every {
            TilkjentYtelseValidering.validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
                any(),
                any(),
            )
        } just runs

        mockkObject(SatsTidspunkt)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2023, 2, 1)

        fagsak = opprettLøpendeFagsak()
        aktørBarn = personidentService.hentOgLagreAktør(randomFnr(), true)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SatsTidspunkt)
        unmockkObject(TilkjentYtelseValidering)
    }

    @Nested
    inner class Satskjøring {
        @Test
        fun `Skal ikke slette satskjøringer dersom en av satskjøringene er ferdig`() {
            // Arrange
            val fagsaker =
                setOf(
                    opprettLøpendeFagsak(),
                    opprettLøpendeFagsak(),
                )

            val satskjøringer =
                listOf(
                    Satskjøring(
                        fagsakId = fagsaker.first().id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                        ferdigTidspunkt = LocalDateTime.now(),
                    ),
                    Satskjøring(
                        fagsakId = fagsaker.last().id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                    ),
                )

            satskjøringRepository.saveAll(satskjøringer)

            // Act
            val sletteSatskjøringerFeil = assertThrows<Feil> { satsendringService.slettSatskjøringer(fagsakIder = fagsaker.map { it.id }.toSet(), satsTidspunkt = SATSENDRINGMÅNED_FEB_2026) }

            // Assert
            assertThat(sletteSatskjøringerFeil.message).isEqualTo("Det finnes en eller flere satskjøringer for fagsaker [${fagsaker.first().id}] som er ferdige. Kan ikke slette.")
        }

        @Test
        fun `Skal slette satskjøringer`() {
            // Arrange
            val fagsaker =
                setOf(
                    opprettLøpendeFagsak(),
                    opprettLøpendeFagsak(),
                )

            val satskjøringer =
                listOf(
                    Satskjøring(
                        fagsakId = fagsaker.first().id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                    ),
                    Satskjøring(
                        fagsakId = fagsaker.last().id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                    ),
                )

            satskjøringRepository.saveAll(satskjøringer)
            // Act
            val slettedeSatskjøringer = satsendringService.slettSatskjøringer(fagsaker.map { it.id }.toSet(), SATSENDRINGMÅNED_FEB_2026)

            // Assert
            assertThat(slettedeSatskjøringer).isEqualTo(fagsaker.map { it.id })
        }

        @Test
        fun `Sletter ikke satskjøringer med andre satstidspunkt enn det som er oppgitt`() {
            // Arrange
            val fagsaker =
                setOf(
                    opprettLøpendeFagsak(),
                )

            val satskjøringer =
                listOf(
                    Satskjøring(
                        fagsakId = fagsaker.first().id,
                        satsTidspunkt = YearMonth.of(2024, 1),
                    ),
                    Satskjøring(
                        fagsakId = fagsaker.first().id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                    ),
                )

            satskjøringRepository.saveAll(satskjøringer)
            // Act
            val slettedeSatskjøringer = satsendringService.slettSatskjøringer(fagsaker.map { it.id }.toSet(), SATSENDRINGMÅNED_FEB_2026)

            // Assert
            assertThat(slettedeSatskjøringer).isEqualTo(fagsaker.map { it.id })

            val satskjøringerIDb = satskjøringRepository.findAll()

            @Suppress("AssertBetweenInconvertibleTypes")
            assertThat(satskjøringerIDb).contains(satskjøringer.first())
        }

        @Test
        fun `finnUferdigeSatskjøringer finner kun uferdige satskjøringer av spesifikk type`() {
            // Arrange
            val fagsaker =
                listOf(
                    opprettLøpendeFagsak(),
                    opprettLøpendeFagsak(),
                    opprettLøpendeFagsak(),
                )

            val satskjøringer =
                listOf(
                    Satskjøring(
                        fagsakId = fagsaker[0].id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                        feiltype = SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT.name,
                    ),
                    Satskjøring(
                        fagsakId = fagsaker[1].id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                        feiltype = SatsendringSvar.BEHANDLING_HAR_FEIL_PÅ_VILKÅR.name,
                    ),
                    Satskjøring(
                        fagsakId = fagsaker[2].id,
                        satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                        ferdigTidspunkt = LocalDateTime.now(),
                    ),
                )

            satskjøringRepository.saveAll(satskjøringer)

            // Act
            val uferdigeSatsendringer =
                satsendringService.finnUferdigeSatskjøringer(
                    feiltyper = listOf(SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT),
                    satsTidspunkt = SATSENDRINGMÅNED_FEB_2026,
                )

            // Assert
            assertThat(uferdigeSatsendringer).isEqualTo(listOf(fagsaker[0].id))
        }
    }

    @Nested
    inner class ÅpenBehandling {
        @Test
        fun `Kan ikke sette åpen behandling på vent når behandlingen akkurat er opprettet`() {
            val behandling = opprettBehandling()
            lagTilkjentAndelOgFerdigstillBehandling(behandling)
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = behandling.fagsak.id,
                    satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                ),
            )

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            opprettBehandling()

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto =
                SatsendringTaskDto(
                    behandling.fagsak.id,
                    YearMonth.of(2023, 3),
                )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT)
        }

        @Test
        fun `Skal sette åpen behandling på maskinell vent hvis den er satt på vent av saksbehandler ved kjøring av satsendring`() {
            val behandling = opprettBehandling()
            lagTilkjentAndelOgFerdigstillBehandling(behandling)
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = behandling.fagsak.id,
                    satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                ),
            )

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            val revurdering = opprettBehandling()
            justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(revurdering)

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto =
                SatsendringTaskDto(
                    behandling.fagsak.id,
                    YearMonth.of(2023, 3),
                )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.SATSENDRING_KJØRT_OK)
        }

        @Test
        fun `Skal sette behandling på vent på maskinell vent hvis den er satt på vent av saksbehandler ved kjøring av satsendring`() {
            val behandling = opprettBehandling()
            lagTilkjentAndelOgFerdigstillBehandling(behandling)
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = behandling.fagsak.id,
                    satsTidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt(),
                ),
            )

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            val revurdering = opprettBehandling()

            settPåVentService.settBehandlingPåVent(
                revurdering.id,
                LocalDate.now(),
                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
            )
            justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(revurdering)

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto =
                SatsendringTaskDto(
                    behandling.fagsak.id,
                    YearMonth.of(2023, 3),
                )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.SATSENDRING_KJØRT_OK)
        }
    }

    private fun justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(behandling: Behandling) {
        jdbcTemplate.update(
            "UPDATE logg SET opprettet_tid = opprettet_tid - interval '12 hours' WHERE fk_behandling_id = ?",
            behandling.id,
        )
        jdbcTemplate.update(
            "UPDATE behandling SET endret_tid = endret_tid - interval '12 hours' WHERE id = ?",
            behandling.id,
        )
    }

    private fun opprettBehandling(): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                NyBehandling(
                    fagsakId = fagsak.id,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                ),
            )
        registrerPersongrunnlag.utførStegOgAngiNeste(
            behandling,
            RegistrerPersongrunnlagDTO(fagsak.aktør.aktivFødselsnummer(), listOf(aktørBarn.aktivFødselsnummer())),
        )
        return behandling
    }

    private fun lagTilkjentAndelOgFerdigstillBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        val avsluttetSteg =
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = StegType.BEHANDLING_AVSLUTTET,
            )
        behandling.behandlingStegTilstand.add(avsluttetSteg)
        with(lagInitiellTilkjentYtelse(behandling, lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))) {
            val andel =
                lagAndelTilkjentYtelse(
                    // Tidspunkt før siste satsendring
                    fom = YearMonth.of(2021, 1),
                    tom =
                        YearMonth.of(
                            2026,
                            5,
                        ),
                    // Tidspunkt etter siste satsendring. Dersom tom er før siste satsendring vil alle testene feile.
                    behandling = behandling,
                    beløp = 10,
                    aktør = aktørBarn,
                    tilkjentYtelse = this,
                )
            andelerTilkjentYtelse.add(andel)
            tilkjentYtelseRepository.saveAndFlush(this)
        }
        behandlingRepository.saveAndFlush(behandling)
    }

    private fun opprettLøpendeFagsak(): Fagsak {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        return fagsakService.lagre(fagsak.copy(status = FagsakStatus.LØPENDE))
    }
}
