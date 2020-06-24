package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KnyttBehandlingTilVilkårResultatMigrering(
        private val behandlingRepository: BehandlingRepository,
        private val behandlingResultatRepository: BehandlingResultatRepository
) {

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    fun migrerVilkårResultat() {
        LOG.info("Migrerer over behandling id til vilkår resultat: start")
        val behandlinger = behandlingRepository.findAll()

        behandlinger.forEach { behandling ->
            val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

            if (behandlingResultat != null) {
                behandlingResultat.personResultater.forEach { personResultat ->
                    personResultat.vilkårResultater.forEach {
                        it.behandlingId = behandling.id
                    }
                }

                behandlingResultatRepository.save(behandlingResultat)
            }
        }

        LOG.info("Migrerer over behandling id til vilkår resultat: slutt")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}
