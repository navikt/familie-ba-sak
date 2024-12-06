package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.common.lagPersonEnkel
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
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
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BehandlingstemaServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val loggService = mockk<LoggService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val tidslinjeService = mockk<VilkårsvurderingTidslinjeService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    private val behandlingstemaService =
        BehandlingstemaService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            loggService = loggService,
            oppgaveService = oppgaveService,
            vilkårsvurderingTidslinjeService = tidslinjeService,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak = fagsak, id = 0L)
    private val dagensDato = LocalDate.of(2024, 8, 1)

    @Nested
    inner class OppdaterBehandlingstemaForRegistrereSøknadTest {
        @Test
        fun `skal ikke oppdatere behandlingstema hvis behandling skal oppdates automatisk`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                    skalBehandlesAutomatisk = true
                )

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaForRegistrereSøknad(
                    behandling = behandling,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            assertThat(oppdatertBehandling).isEqualTo(behandling)
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere behandlingstema for registrering av søknad når det er en endring`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                behandlingstemaService.oppdaterBehandlingstemaForRegistrereSøknad(
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
                    behandlingKategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Act
            val oppdatertBehandling =
                behandlingstemaService.oppdaterBehandlingstemaForRegistrereSøknad(
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
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                behandlingstemaService.oppdaterBehandlingstemaForRegistrereSøknad(
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
        fun `skal oppdatere både saksbehandlet kategori og underkategori på behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = BehandlingKategori.NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.ORDINÆR,
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.UtvidetBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.EØS.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere kun saksbehandlet kategori behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = BehandlingKategori.NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.UTVIDET,
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.UtvidetBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.EØS.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.EØS)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere kun saksbehandlet underkategori behandling`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    nyKategori = BehandlingKategori.NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = BehandlingKategori.NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.UTVIDET,
                    nyKategori = BehandlingKategori.NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                )
            }
            assertThat(patchedOppgave?.behandlingstema).isEqualTo(Behandlingstema.OrdinærBarnetrygd.value)
            assertThat(patchedOppgave?.behandlingstype).isEqualTo(Behandlingstype.NASJONAL.value)
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal ikke oppdatere kategori eller underkategori om de er uendret fra det some allerede finnes i databasen`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    nyKategori = BehandlingKategori.NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 0) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 0) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = BehandlingKategori.NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.UTVIDET,
                    nyKategori = BehandlingKategori.NASJONAL,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave).isNull()
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere patche oppgave om det allerde er i synk med databaseendringen`() {
            // Arrange
            val behandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )

            // Assert
            verify(exactly = 1) { behandlingHentOgPersisterService.lagreEllerOppdater(any()) }
            verify(exactly = 1) { oppgaveService.patchOppgaverForBehandling(any(), any()) }
            verify(exactly = 1) {
                loggService.opprettEndretBehandlingstema(
                    behandling = oppdatertBehandling,
                    forrigeKategori = BehandlingKategori.NASJONAL,
                    forrigeUnderkategori = BehandlingUnderkategori.ORDINÆR,
                    nyKategori = BehandlingKategori.EØS,
                    nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                )
            }
            assertThat(patchedOppgave).isNull()
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.EØS)
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
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
                    behandlingKategori = BehandlingKategori.NASJONAL,
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
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `skal oppdatere behandling ved utledet kategori og underkategori`() {
            // Arrange
            val aktivBehandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.EØS,
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
            assertThat(oppdatertBehandling.kategori).isEqualTo(BehandlingKategori.NASJONAL)
            assertThat(oppdatertBehandling.underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }

    @Nested
    inner class FinnKategoriTest {
        @Test
        fun `skal returnere NASJONAL når hverken aktiv eller siste vedtatte behandling finnes`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.NASJONAL)
        }

        @Test
        fun `skal returnere siste vedtatt behandling sin kategori når det ikke finnes en aktiv behandling men en siste vedtatt behandling finnes`() {
            // Arrange
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling

            // Act
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.EØS)
        }

        @Test
        fun `skal returnere NASJONAL når ingen tidslinje blir funnet for vilkårsvurderingen til den aktive behandlingen og den siste vedtatte behandlingen finnes ikke`() {
            // Arrange
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null

            // Act
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.NASJONAL)
        }

        @Test
        fun `skal returnere kategorien til den siste vedtatte behandlingen når ingen tidslinje blir funnet for vilkårsvurderingen til den aktive behandlingen men den siste vedtatte behandlingen finnes`() {
            // Arrange
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.NASJONAL)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns aktivBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { tidslinjeService.hentTidslinjer(BehandlingId(aktivBehandling.id)) } returns null

            // Act
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.EØS)
        }

        @Test
        fun `skal utlede kategori EØS et barn har en løpende EØS periode`() {
            // Arrange
            val stønadFom = LocalDate.now().minusMonths(1)
            val stønadTom = LocalDate.now().plusYears(2)

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
                                        vurderesEtterFn = { if (it in listOf(Vilkår.BOR_MED_SØKER, Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)) Regelverk.EØS_FORORDNINGEN else null },
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
                                        vurderesEtterFn = { if (it in listOf(Vilkår.BOR_MED_SØKER, Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)) Regelverk.NASJONALE_REGLER else null },
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
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.EØS)
        }

        @Test
        fun `skal utlede kategori NASJONAL når et barn har en løpenede nasjonal periode`() {
            // Arrange
            val stønadFom = LocalDate.now().minusMonths(1)
            val stønadTom = LocalDate.now().plusYears(2)

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
                                        vurderesEtterFn = { if (it in listOf(Vilkår.BOR_MED_SØKER, Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)) Regelverk.NASJONALE_REGLER else null },
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
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Assert
            assertThat(kategori).isEqualTo(BehandlingKategori.NASJONAL)
        }

        @Test
        fun `skal utlede kategorien til siste vedtatte behandling når det ikke finnes en løpende NASJONAL eller løpende EØS periode på barnas vilkårsvurderingtidslinje for den aktive behandling men den siste vedtatte behandling finnes`() {
            // Arrange
            val stønadFom = LocalDate.now().minusMonths(3)
            val stønadTom = LocalDate.now().minusMonths(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
            val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

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
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(BehandlingKategori.EØS)
        }

        @Test
        fun `skal utlede kategori NASJONAL når det ikke finnes en løpende NASJONAL eller løpende EØS periode på barnas vilkårsvurderingtidslinje for den aktive behandlingen og siste vedtatte behandling finnes ikke`() {
            // Arrange
            val stønadFom = LocalDate.now().minusMonths(3)
            val stønadTom = LocalDate.now().minusMonths(2)

            val søker = lagPersonEnkel(personType = PersonType.SØKER, aktør = randomAktør())
            val barn1 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)
            val barn2 = lagPersonEnkel(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = stønadFom)

            val fagsak = lagFagsak(aktør = søker.aktør)
            val aktivBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

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
            val kategori = behandlingstemaService.finnKategori(fagsak.id)

            // Act
            assertThat(kategori).isEqualTo(BehandlingKategori.NASJONAL)
        }

        private fun lagAlleVilkårResultater(
            behandling: Behandling,
            personType: PersonType,
            personResultat: PersonResultat,
            stønadFom: LocalDate,
            stønadTom: LocalDate,
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
    inner class FinnLøpendeUnderkategoriTest {
        @Test
        fun `skal returnere null om ingen vedtatt behandling for fagsaken blir funnet`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnLøpendeUnderkategori(fagsak.id)

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
            val underkategori = behandlingstemaService.finnLøpendeUnderkategori(fagsak.id)

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
            val underkategori = behandlingstemaService.finnLøpendeUnderkategori(fagsak.id)

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
            val underkategori = behandlingstemaService.finnLøpendeUnderkategori(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }

    @Nested
    inner class FinUnderkategoriFraInneværendeBehandlingTest {
        @Test
        fun `skal returnere ORDINÆR om ingen aktiv og åpen behandling blir funnet for fagsak`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraInneværendeBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal returnere ORDINÆR om ingen vilkårsvurdering blir funnet for behandlingen`() {
            // Arrange
            every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsak.id) } returns behandling
            every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id) } returns null

            // Act
            val underkategori = behandlingstemaService.finnUnderkategoriFraInneværendeBehandling(fagsak.id)

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
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
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
            val underkategori = behandlingstemaService.finnUnderkategoriFraInneværendeBehandling(fagsak.id)

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
                                            vilkårType = Vilkår.BOR_MED_SØKER,
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
            val underkategori = behandlingstemaService.finnUnderkategoriFraInneværendeBehandling(fagsak.id)

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
                                            vurderesEtter = Regelverk.NASJONALE_REGLER,
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
            val underkategori = behandlingstemaService.finnUnderkategoriFraInneværendeBehandling(fagsak.id)

            // Assert
            assertThat(underkategori).isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }
    }
}
