package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.junit.jupiter.api.Test

class PreutfyllVilkårServiceTest {
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService = mockk(relaxed = true)
    private val preutfyllBorMedSøkerService: PreutfyllBorMedSøkerService = mockk(relaxed = true)
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService = mockk(relaxed = true)
    private val preutfyllBosattIRiketForFødselshendelserService: PreutfyllBosattIRiketForFødselshendelserService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    val preutfyllVilkårService =
        PreutfyllVilkårService(
            preutfyllLovligOppholdService,
            preutfyllBorMedSøkerService,
            preutfyllBosattIRiketService,
            preutfyllBosattIRiketForFødselshendelserService,
            persongrunnlagService,
            behandlingHentOgPersisterService,
        )

    @Test
    fun `Skal ikke kjøre noe preutfylling for behandlinger i fagsak type skjermet barn`() {
        // Arrange
        val behandling =
            lagBehandling(
                fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN),
            )

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 0) { preutfyllLovligOppholdService.preutfyllLovligOpphold(any(), any()) }
        verify(exactly = 0) { preutfyllBosattIRiketService.preutfyllBosattIRiket(any(), any()) }
        verify(exactly = 0) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(any(), any()) }
    }

    @Test
    fun `Skal ikke kjøre preutfylling for EØS-behandling`() {
        // Arrange
        val behandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)
        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 0) { preutfyllLovligOppholdService.preutfyllLovligOpphold(any(), any()) }
        verify(exactly = 0) { preutfyllBosattIRiketService.preutfyllBosattIRiket(any(), any()) }
        verify(exactly = 0) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(any(), any()) }
    }

    @Test
    fun `Skal preutfylle alle vilkår med alle aktører for førstegangsbehandling`() {
        // Arrange
        val fagsak = lagFagsak()
        val behandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)
        every { persongrunnlagService.oppdaterRegisteropplysninger(behandling.id) } returns mockk()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)
        val alleAktører = listOf(søker.aktør, barn.aktør)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 1) { preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering, alleAktører) }
        verify(exactly = 1) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, alleAktører) }
        verify(exactly = 1) { preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, alleAktører) }
    }

    @Test
    fun `Skal ikke kjøre preutfylling for revurdering med årsak som ikke er søknad`() {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            )
        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 0) { preutfyllLovligOppholdService.preutfyllLovligOpphold(any(), any()) }
        verify(exactly = 0) { preutfyllBosattIRiketService.preutfyllBosattIRiket(any(), any()) }
        verify(exactly = 0) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(any(), any()) }
    }

    @Test
    fun `Skal preutfylle bare for nye barn i revurdering med årsak søknad`() {
        // Arrange
        val fagsak = lagFagsak()
        val behandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING, årsak = BehandlingÅrsak.SØKNAD)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)

        val søker = lagPerson(type = PersonType.SØKER)
        val eksisterendeBarn = lagPerson(type = PersonType.BARN)
        val nyttBarn = lagPerson(type = PersonType.BARN)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(behandling.id, søker, eksisterendeBarn, nyttBarn)
        every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns
            lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søker, eksisterendeBarn)
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
        every { persongrunnlagService.oppdaterRegisteropplysninger(behandling.id) } returns mockk()

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)
        val nyttBarnAktør = nyttBarn.aktør

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 1) { preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering, listOf(nyttBarnAktør)) }
        verify(exactly = 1) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, listOf(nyttBarnAktør)) }
        verify(exactly = 1) { preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, listOf(nyttBarnAktør)) }
    }

    @Test
    fun `Skal ikke preutfylle for revurdering med årsak søknad hvis ingen nye barn`() {
        // Arrange
        val fagsak = lagFagsak()
        val behandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.REVURDERING, årsak = BehandlingÅrsak.SØKNAD)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, årsak = BehandlingÅrsak.SØKNAD)

        val søker = lagPerson(type = PersonType.SØKER)
        val eksisterendeBarn = lagPerson(type = PersonType.BARN)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns
            lagTestPersonopplysningGrunnlag(behandling.id, søker, eksisterendeBarn)
        every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns
            lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søker, eksisterendeBarn)
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert
        verify(exactly = 0) { persongrunnlagService.oppdaterRegisteropplysninger(any()) }
        verify(exactly = 0) { preutfyllLovligOppholdService.preutfyllLovligOpphold(any(), any()) }
        verify(exactly = 0) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(any(), any()) }
        verify(exactly = 0) { preutfyllBosattIRiketService.preutfyllBosattIRiket(any(), any()) }
    }
}
