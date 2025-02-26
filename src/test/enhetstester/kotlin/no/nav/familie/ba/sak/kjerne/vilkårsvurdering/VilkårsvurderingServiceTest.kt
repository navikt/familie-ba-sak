package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.every
import io.mockk.mockk
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
    fun `oppdaterVilkårVedDødsfall skal sette tom dato til dødsfallsdato dersom dødsfallsdato er tidligere enn nåværende tom`() {
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

        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == dødsfallsDato }, Is(true))
    }

    @Test
    fun `oppdaterVilkårVedDødsfall skal ikke sette tom dato til dødsfallsdato dersom dødsfallsdato er senere enn nåværende tom`() {
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

        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == vilkårTomDato }, Is(true))
    }

    @Test
    fun `oppdaterVilkårVedDødsfall skal ikke sette tom dato til dødsfallsdato dersom tom dato ikke allerede er satt`() {
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

        vilkårsvurderingService.oppdaterVilkårVedDødsfall(behandlingId = BehandlingId(behandling.id), dødsfallsDato, aktør)

        val vilkårResultater = vilkårsvurdering.personResultater.single().vilkårResultater

        assertThat(vilkårResultater.all { it.periodeTom == null }, Is(true))
    }
}
