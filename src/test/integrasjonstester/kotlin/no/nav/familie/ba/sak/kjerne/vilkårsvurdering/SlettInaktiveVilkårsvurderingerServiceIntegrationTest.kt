package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SlettInaktiveVilkårsvurderingerServiceIntegrationTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    @Autowired private val slettInaktiveVilkårsvurderingerService: SlettInaktiveVilkårsvurderingerService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `slettBatchMedInaktiveVilkårsvurderinger skal slette batchstørrelsen i stigende id-rekkefølge etter gitt id og returnere id-ene`() {
        // Arrange
        val behandling = lagreBehandling()
        val inaktiveIder = (1..3).map { vilkårsvurderingRepository.save(lagVilkårsvurdering(behandling = behandling, aktiv = false)).id }.sorted()

        // Act
        val slettedeIder = slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(etterId = inaktiveIder.first() - 1, batchStørrelse = 2)

        // Assert
        assertThat(slettedeIder).containsExactly(inaktiveIder[0], inaktiveIder[1])
        assertThat(vilkårsvurderingRepository.findById(inaktiveIder[0])).isEmpty
        assertThat(vilkårsvurderingRepository.findById(inaktiveIder[1])).isEmpty
        assertThat(vilkårsvurderingRepository.findById(inaktiveIder[2])).isPresent
    }

    private fun lagreBehandling(): Behandling {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        return behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
    }
}
