package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class VilkårsvurderingServiceTest {
    private val sanityService = mockk<SanityService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    private val vilkårsvurderingService = VilkårsvurderingService(vilkårsvurderingRepository, sanityService)

    @Test
    fun `lagreNyOgSlettGammel skal slette eksisterende aktiv vilkårsvurdering før den nye lagres`() {
        // Arrange
        val behandling = lagBehandling()
        val gammelVilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = randomAktør(),
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
            )
        val nyVilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = randomAktør(),
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns gammelVilkårsvurdering
        justRun { vilkårsvurderingRepository.delete(gammelVilkårsvurdering) }
        every { vilkårsvurderingRepository.save(nyVilkårsvurdering) } returns nyVilkårsvurdering

        // Act
        val lagretVilkårsvurdering = vilkårsvurderingService.lagreNyOgSlettGammel(nyVilkårsvurdering)

        // Assert
        assertThat(lagretVilkårsvurdering, Is(nyVilkårsvurdering))
        verify { vilkårsvurderingRepository.delete(gammelVilkårsvurdering) }
        verify { vilkårsvurderingRepository.save(nyVilkårsvurdering) }
    }

    @Test
    fun `lagreNyOgSlettGammel skal ikke slette noe dersom det ikke finnes en aktiv vilkårsvurdering`() {
        // Arrange
        val behandling = lagBehandling()
        val nyVilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = randomAktør(),
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
            )

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns null
        every { vilkårsvurderingRepository.save(nyVilkårsvurdering) } returns nyVilkårsvurdering

        // Act
        val lagretVilkårsvurdering = vilkårsvurderingService.lagreNyOgSlettGammel(nyVilkårsvurdering)

        // Assert
        assertThat(lagretVilkårsvurdering, Is(nyVilkårsvurdering))
        verify(exactly = 0) { vilkårsvurderingRepository.delete(any()) }
    }

    @Test
    fun `oppdaterVilkårVedDødsfall skal sette tom dato til dødsfallsdato dersom dødsfallsdato er tidligere enn nåværende tom`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)
        val aktør = randomAktør()
        val vilkårFomDato = LocalDate.of(2000, 1, 1)
        val vilkårTomDato = LocalDate.of(2020, 1, 1)
        val dødsfallsDato = LocalDate.of(2015, 1, 1)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = aktør,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
                søkerPeriodeFom = vilkårFomDato,
                søkerPeriodeTom = vilkårTomDato,
            )

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns vilkårsvurdering

        // Act
        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        // Assert
        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == dødsfallsDato }, Is(true))
    }

    @Test
    fun `oppdaterVilkårVedDødsfall skal ikke sette tom dato til dødsfallsdato dersom dødsfallsdato er senere enn nåværende tom`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)
        val aktør = randomAktør()
        val vilkårFomDato = LocalDate.of(2000, 1, 1)
        val vilkårTomDato = LocalDate.of(2020, 1, 1)
        val dødsfallsDato = LocalDate.of(2022, 1, 1)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = aktør,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
                søkerPeriodeFom = vilkårFomDato,
                søkerPeriodeTom = vilkårTomDato,
            )

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns vilkårsvurdering

        // Act
        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        // Assert
        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == vilkårTomDato }, Is(true))
    }

    @Test
    fun `oppdaterVilkårVedDødsfall skal ikke sette tom dato til dødsfallsdato dersom tom dato ikke allerede er satt`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)
        val aktør = randomAktør()
        val vilkårFomDato = LocalDate.of(2000, 1, 1)
        val dødsfallsDato = LocalDate.of(2022, 1, 1)

        val vilkårsvurdering =
            lagVilkårsvurdering(
                søkerAktør = aktør,
                behandling = behandling,
                resultat = Resultat.IKKE_VURDERT,
                søkerPeriodeFom = vilkårFomDato,
                søkerPeriodeTom = null,
            )

        every { vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = behandling.id) } returns vilkårsvurdering

        // Act
        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        // Assert
        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == null }, Is(true))
    }
}
