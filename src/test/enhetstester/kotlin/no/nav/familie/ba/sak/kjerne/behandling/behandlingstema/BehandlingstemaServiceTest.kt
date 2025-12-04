package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori.EØS
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori.NASJONAL
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class BehandlingstemaServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val loggService = mockk<LoggService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val tidslinjeService = mockk<VilkårsvurderingTidslinjeService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()

    private val dagensDato = LocalDate.of(2024, 10, 1)

    private val behandlingstemaService =
        BehandlingstemaService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            oppgaveService = oppgaveService,
            vilkårsvurderingTidslinjeService = tidslinjeService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(dagensDato),
            integrasjonKlient = integrasjonKlient,
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak = fagsak, id = 0L)

    @Nested
    inner class OppdaterBehandlingstemaFraRegistrereSøknadStegTest {
        @Test
        fun `skal ikke oppdatere behandlingstema hvis behandling skal oppdates automatisk`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                    skalBehandlesAutomatisk = true,
                )

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(
                    behandling = behandling,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            assertThat(oppdatertBehandling).isEqualTo(behandling)
            assertThat(oppdatertBehandling.kategori).isEqualTo(NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere behandlingstema for registrering av søknad når det er en endring`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.UtvidetBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(
                    behandling = behandling,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(behandling.kategori)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal oppdatere behandlingstema for registrering av søknad når ikke det er en endring`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(
                    behandling = behandling,
                    nyUnderkategori = behandling.underkategori,
                )

            // Assert
            verify(exactly = 0) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 0) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            assertThat(oppdatertBehandling.kategori).isEqualTo(behandling.kategori)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere behandlingstema i database men ikke patch oppgave om det allerede er i synk`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.OrdinærBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaFraRegistrereSøknadSteg(
                    behandling = behandling,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            assertThat(patchedOppgave).isNull()
            assertThat(oppdatertBehandling.kategori).isEqualTo(behandling.kategori)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }

    @Nested
    inner class OppdaterSaksbehandletBehandlingstemaTest {
        @Test
        fun `skal kaste exception om behandlingen skal behandles automatisk men saksbehandler prøver å oppdatere behandlingestema`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                    skalBehandlesAutomatisk = true,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                        behandling,
                        nyKategori = EØS,
                        nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                    )
                }
            assertThat(exception.message).isEqualTo("Kan ikke oppdatere behandlingstema manuelt på behandlinger som skal behandles automatisk.")
        }

        @Test
        fun `skal oppdatere både saksbehandlet kategori og underkategori på behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.OrdinærBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            every { loggService.opprettEndretBehandlingstema(any(), any(), any(), any(), any()) } just runs

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                    behandling = behandling,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.ORDINÆR,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.UtvidetBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.EØS.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(EØS)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere kun saksbehandlet kategori behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.UtvidetBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            every { loggService.opprettEndretBehandlingstema(any(), any(), any(), any(), any()) } just runs

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                    behandling = behandling,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.UTVIDET,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.UtvidetBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.EØS.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(EØS)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere kun saksbehandlet underkategori behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.UtvidetBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            every { loggService.opprettEndretBehandlingstema(any(), any(), any(), any(), any()) } just runs

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                    behandling = behandling,
                    nyKategori = NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.UTVIDET,
                    nyKategori = NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal ikke oppdatere kategori eller underkategori om de er uendret fra det some allerede finnes i databasen`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.UtvidetBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            every { loggService.opprettEndretBehandlingstema(any(), any(), any(), any(), any()) } just runs

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                    behandling = behandling,
                    nyKategori = NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 0) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 0) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 0) {
                loggService.opprettEndretBehandlingstema(
                    behandling = any(),
                    forrigeKategori = any(),
                    forrigeUnderkategori = any(),
                    nyKategori = any(),
                    nyUnderkategori = any(),
                )
            }
            assertThat(patchedOppgave).isNull()
            assertThat(oppdatertBehandling.kategori).isEqualTo(NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere patche oppgave om det allerde er i synk med databaseendringen`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                )

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.UtvidetBarnetrygd.value,
                    behandlingstype = Behandlingstype.EØS.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(behandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(behandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            every { loggService.opprettEndretBehandlingstema(any(), any(), any(), any(), any()) } just runs

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterSaksbehandletBehandlingstema(
                    behandling = behandling,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.ORDINÆR,
                    nyKategori = EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave).isNull()
            assertThat(oppdatertBehandling.kategori).isEqualTo(EØS)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }
    }

    @Nested
    inner class OppdaterBehandlingstemaForVilkårTest {
        @Test
        fun `skal ikke oppdatere kategori og underkategori om behandling skal behandles automatisk`() {
            // Arrange
            val aktivBehandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    skalBehandlesAutomatisk = true,
                )

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                    behandling = aktivBehandling,
                )

            // Assert
            verify(exactly = 0) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 0) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            assertThat(oppdatertBehandling).isEqualTo(aktivBehandling)
        }

        @Test
        fun `skal sette overstyr underkategori`() {
            // Arrange
            val aktivBehandling =
                lagBehandling(
                    behandlingKategori = NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(aktivBehandling.id) } returns null

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.OrdinærBarnetrygd.value,
                    behandlingstype = Behandlingstype.NASJONAL.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(aktivBehandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(aktivBehandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                    behandling = aktivBehandling,
                    overstyrtUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }

            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.UtvidetBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere behandling ved utledet kategori og underkategori`() {
            // Arrange
            val aktivBehandling =
                lagBehandling(
                    behandlingKategori = EØS,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(aktivBehandling.id) } returns null

            val oppgave =
                Oppgave(
                    behandlingstema = Behandlingstema.BarnetrygdEØS.value,
                    behandlingstype = Behandlingstype.EØS.value,
                )
            val patchOppgaveCallback = slot<(Oppgave) -> Oppgave>()
            var patchedOppgave: Oppgave? = null

            every { behandlingHentOgPersisterService.lagreEllerOppdater(aktivBehandling) } returnsArgument 0
            every { oppgaveService.patchOppgaverForBehandling(eq(aktivBehandling), capture(patchOppgaveCallback)) } answers {
                patchedOppgave = patchOppgaveCallback.captured.invoke(oppgave)
            }
            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaForVilkår(
                    behandling = aktivBehandling,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }

            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }

    @Nested
    inner class FinnBehandlingKategoriTest {
        @Test
        fun `skal returnere NASJONAL når hverken aktiv eller siste vedtatte behandling finnes`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal returnere siste vedtatt behandling sin kategori når det ikke finnes en aktiv behandling men en siste vedtatt behandling finnes`() {
            // Arrange
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal returnere NASJONAL når ingen tidslinje blir funnet for vilkårsvurderingen til den aktive behandlingen og den siste vedtatte behandlingen finnes ikke`() {
            // Arrange
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal returnere kategorien til den siste vedtatte behandlingen når ingen tidslinje blir funnet for vilkårsvurderingen til den aktive behandlingen men den siste vedtatte behandlingen finnes`() {
            // Arrange
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = NASJONAL)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal utlede kategori EØS et barn har en løpende EØS periode`() {
            // Arrange
            val stønadFom = dagensDato.minusMonths(1)
            val stønadTom = dagensDato.plusYears(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = {
                                            if (it in
                                                listOf(
                                                    BOR_MED_SØKER,
                                                    BOSATT_I_RIKET,
                                                    LOVLIG_OPPHOLD,
                                                )
                                            ) {
                                                EØS_FORORDNINGEN
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = {
                                            if (it in
                                                listOf(
                                                    BOR_MED_SØKER,
                                                    BOSATT_I_RIKET,
                                                    LOVLIG_OPPHOLD,
                                                )
                                            ) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal ta utgangspunkt i regelverk neste måned når regelverk bytter i inneværende måned`() {
            // Arrange
            val vilkårFom = LocalDate.of(2023, 1, 1)
            val overgangTilEØSForBarn2 = dagensDato

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = null,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = null,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = overgangTilEØSForBarn2.minusDays(1),
                                    ) +
                                        lagAlleVilkårResultater(
                                            behandling = aktivBehandling,
                                            personType = PersonType.BARN,
                                            personResultat = personResultat,
                                            stønadFom = overgangTilEØSForBarn2,
                                            stønadTom = null,
                                            vurderesEtterFn = {
                                                if (it in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                    EØS_FORORDNINGEN
                                                } else {
                                                    null
                                                }
                                            },
                                        )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal ikke inkludere regelverk for barn som er opphørt neste måned`() {
            // Arrange
            val vilkårFom = LocalDate.of(2023, 1, 1)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = null,
                                        vurderesEtterFn = { vilkår ->
                                            if (vilkår in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = null,
                                        vurderesEtterFn = { vilkår ->
                                            if (vilkår in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = dagensDato,
                                        vurderesEtterFn = {
                                            if (it in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                EØS_FORORDNINGEN
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal ta utgangspunkt i regelverk for vilkår inneværende måned hvis vilkår for alle barn er opphørt neste måned`() {
            // Arrange
            val vilkårFom = LocalDate.of(2023, 1, 1)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør())

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = dagensDato,
                                        vurderesEtterFn = { vilkår ->
                                            if (vilkår in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = dagensDato,
                                        vurderesEtterFn = { vilkår ->
                                            if (vilkår in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = vilkårFom,
                                        stønadTom = dagensDato,
                                        vurderesEtterFn = { vilkår ->
                                            if (vilkår in setOf(BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD)) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal utlede kategori NASJONAL når et barn har en løpenede nasjonal periode`() {
            // Arrange
            val stønadFom = dagensDato.minusMonths(1)
            val stønadTom = dagensDato.plusYears(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = {
                                            if (it in
                                                listOf(
                                                    BOR_MED_SØKER,
                                                    BOSATT_I_RIKET,
                                                    LOVLIG_OPPHOLD,
                                                )
                                            ) {
                                                NASJONALE_REGLER
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal utlede kategorien til siste vedtatte behandling når det ikke finnes en løpende NASJONAL eller løpende EØS periode på barnas vilkårsvurderingtidslinje for den aktive behandling men den siste vedtatte behandling finnes`() {
            // Arrange
            val stønadFom = dagensDato.minusMonths(3)
            val stønadTom = dagensDato.minusMonths(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = { null },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = { null },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal utlede kategori til aktiv behandling dersom det ikke finnes en løpende NASJONAL eller løpende EØS periode på barnas vilkårsvurderingtidslinje for den aktive behandlingen og siste vedtatte behandling ikke finnes`() {
            // Arrange
            val stønadFom = dagensDato.minusMonths(3)
            val stønadTom = dagensDato.minusMonths(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.SØKER,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn1.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = { null },
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn2.aktør,
                                lagVilkårResultater = { personResultat ->
                                    lagAlleVilkårResultater(
                                        behandling = aktivBehandling,
                                        personType = PersonType.BARN,
                                        personResultat = personResultat,
                                        stønadFom = stønadFom,
                                        stønadTom = stønadTom,
                                        vurderesEtterFn = { null },
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(EØS)
        }

        @Test
        fun `skal utlede kategori NASJONAl`() {
            // Arrange
            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør(), fødselsdato = LocalDate.of(1983, 6, 22))
            val barn = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2022, 11, 15))

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = NASJONAL)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 1, 15),
                                            periodeTom = LocalDate.of(2024, 8, 31),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2024, 9, 1),
                                            periodeTom = null,
                                            vurderesEtter = NASJONALE_REGLER,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 1, 15),
                                            periodeTom = LocalDate.of(2024, 8, 31),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2024, 9, 1),
                                            periodeTom = null,
                                            vurderesEtter = NASJONALE_REGLER,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 11, 15),
                                            periodeTom = LocalDate.of(2024, 8, 31),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2024, 9, 1),
                                            periodeTom = null,
                                            vurderesEtter = NASJONALE_REGLER,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 11, 15),
                                            periodeTom = LocalDate.of(2024, 8, 31),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2024, 9, 1),
                                            periodeTom = null,
                                            vurderesEtter = NASJONALE_REGLER,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 11, 15),
                                            periodeTom = LocalDate.of(2024, 8, 31),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOR_MED_SØKER,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2024, 9, 1),
                                            periodeTom = null,
                                            vurderesEtter = NASJONALE_REGLER,
                                            vilkårType = BOR_MED_SØKER,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 11, 15),
                                            periodeTom = LocalDate.of(2040, 11, 14),
                                            vurderesEtter = null,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2022, 11, 15),
                                            periodeTom = null,
                                            vurderesEtter = null,
                                            vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(NASJONAL)
        }

        @Test
        fun `skal utlede kategori EØS når det ikke finnes oppfylte vilkår resultater for dagens dato og det ikke finnes en tidligere vedtatt behandling`() {
            // Arrange
            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør(), fødselsdato = LocalDate.of(1979, 6, 26))
            val barn = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2006, 4, 21))

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = NASJONAL)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = EØS)

            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = aktivBehandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2021, 4, 30),
                                            periodeTom = LocalDate.of(2021, 6, 30),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.IKKE_OPPFYLT,
                                            periodeFom = LocalDate.of(2021, 7, 1),
                                            periodeTom = LocalDate.of(2021, 8, 29),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2021, 8, 30),
                                            periodeTom = LocalDate.of(2021, 9, 30),
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2013, 1, 31),
                                            periodeTom = null,
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2013, 1, 31),
                                            periodeTom = null,
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOSATT_I_RIKET,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2013, 1, 31),
                                            periodeTom = null,
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = LOVLIG_OPPHOLD,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2013, 1, 31),
                                            periodeTom = null,
                                            vurderesEtter = EØS_FORORDNINGEN,
                                            vilkårType = BOR_MED_SØKER,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2006, 4, 21),
                                            periodeTom = LocalDate.of(2024, 4, 20),
                                            vurderesEtter = null,
                                            vilkårType = Vilkår.UNDER_18_ÅR,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            resultat = Resultat.OPPFYLT,
                                            periodeFom = LocalDate.of(2006, 4, 21),
                                            periodeTom = null,
                                            vurderesEtter = null,
                                            vilkårType = Vilkår.GIFT_PARTNERSKAP,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val vilkårsvurderingTidslinjer =
                VilkårsvurderingTidslinjer(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn),
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns vilkårsvurderingTidslinjer

            // Act
            val kategori = behandlingstemaService.finnBehandlingKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(EØS)
        }

        private fun lagAlleVilkårResultater(
            behandling: Behandling,
            personType: PersonType,
            personResultat: PersonResultat,
            stønadFom: LocalDate,
            stønadTom: LocalDate?,
            vurderesEtterFn: (vilkår: Vilkår) -> Regelverk? = { vilkår -> personResultat.let { vilkår.defaultRegelverk(it.vilkårsvurdering.behandling.kategori) } },
        ) = Vilkår
            .hentVilkårFor(
                personType = personType,
                fagsakType = behandling.fagsak.type,
                behandlingUnderkategori = behandling.underkategori,
            ).map { vilkår ->
                lagVilkårResultat(
                    personResultat = personResultat,
                    periodeFom = stønadFom,
                    periodeTom = stønadTom,
                    vilkårType = vilkår,
                    resultat = Resultat.OPPFYLT,
                    behandlingId = behandling.id,
                    vurderesEtter = vurderesEtterFn(vilkår),
                )
            }.toSet()
    }

    @Nested
    inner class FinnLøpendeUnderkategoriFraForrigeVedtatteBehandlingTest {
        @Test
        fun `skal returnere null om ingen vedtatt behandling for fagsaken blir funnet`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isNull()
        }

        @Test
        fun `skal returnere UTVIDET om det finnes en andel tilkjent ytelse som er løpende og er av ytelsestype UTVIDET_BARNETRYGD`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.now().minusMonths(1),
                        tom = YearMonth.now().plusMonths(1),
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    ),
                )
            // Act
            val underkategori = behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal returnere ORDINÆR om det finnes en andel tilkjent ytelse som er løpende men ikke av ytelsestype UTVIDET_BARNETRYGD`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.now().minusMonths(1),
                        tom = YearMonth.now().plusMonths(1),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                )
            // Act
            val underkategori = behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal returnere ORDINÆR om det finnes en andel tilkjent ytelse som ikke er løpende men er av ytelsestype UTVIDET_BARNETRYGD`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.now().minusMonths(2),
                        tom = YearMonth.now().minusMonths(1),
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                )
            // Act
            val underkategori = behandlingstemaService.finnLøpendeUnderkategoriFraForrigeVedtatteBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }

    @Nested
    inner class FinnUnderkategoriFraAktivBehandlingTest {
        @Test
        fun `skal returnere ORDINÆR om ingen aktiv og åpen behandling blir funnet for fagsak`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraAktivBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal returnere ORDINÆR om ingen vilkårsvurdering blir funnet for behandlingen`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns behandling
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraAktivBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `Skal utlede UTVIDET dersom minst ett vilkår i har blitt behandlet i inneværende behandling`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = fagsak.aktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                            periodeFom = dagensDato.minusMonths(1),
                                            periodeTom = dagensDato,
                                        ),
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = BOSATT_I_RIKET,
                                            periodeFom = dagensDato.plusDays(1),
                                            periodeTom = dagensDato.plusMonths(2),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns behandling
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns vilkårsvurdering

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraAktivBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `Skal utlede ORDINÆR dersom ingen utvidet barnetrygd vilkår i har blitt behandlet i inneværende behandling`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = fagsak.aktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = behandling.id,
                                            vilkårType = BOR_MED_SØKER,
                                            periodeFom = dagensDato.minusMonths(1),
                                            periodeTom = dagensDato,
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns behandling
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns vilkårsvurdering

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraAktivBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `Skal utlede ORDINÆR dersom UTVIDET vilkåret ble behandlet i annen behandling`() {
            // Arrange
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = { vilkårsvurdering ->
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = vilkårsvurdering,
                                aktør = fagsak.aktør,
                                lagVilkårResultater = {
                                    setOf(
                                        lagVilkårResultat(
                                            personResultat = it,
                                            behandlingId = 1337L,
                                            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                                            vurderesEtter = NASJONALE_REGLER,
                                            periodeFom = dagensDato.minusMonths(1),
                                            periodeTom = dagensDato.plusMonths(1),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns behandling
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns vilkårsvurdering

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraAktivBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }
}
