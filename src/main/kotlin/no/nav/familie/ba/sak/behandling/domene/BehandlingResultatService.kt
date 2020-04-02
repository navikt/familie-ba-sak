package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.IllegalStateException

@Service
class BehandlingResultatService(
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val loggService: LoggService
) {

    fun hentBehandlingResultatTypeFraBehandling(behandlingId: Long): BehandlingResultatType {
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId)
                                 ?: return BehandlingResultatType.IKKE_VURDERT

        return behandlingResultat.hentSamletResultat()
    }

    fun hentAktivForBehandling(behandlingId: Long): BehandlingResultat? {
        return behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun lagreNyOgDeaktiverGammel(behandlingResultat: BehandlingResultat): BehandlingResultat {
        val aktivBehandlingResultat = hentAktivForBehandling(behandlingResultat.behandling.id)
        if (aktivBehandlingResultat != null) {
            behandlingResultatRepository.saveAndFlush(aktivBehandlingResultat.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter behandling resultat $behandlingResultat")
        loggService.opprettVilkårsvurderingLogg(behandlingResultat.behandling, aktivBehandlingResultat, behandlingResultat)
        return behandlingResultatRepository.save(behandlingResultat)
    }

    fun settBegrunnelseForVilkårsvurderingerPåAktiv(behandlingId: Long, begrunnelse: String): BehandlingResultat {
        val aktivBehandlingResultat = hentAktivForBehandling(behandlingId) ?: throw IllegalStateException("Ingen aktiv BehandlingResultat når begrunnelse skal settes på behandling med ID: $behandlingId}\"")
        aktivBehandlingResultat.periodeResultater.forEach { periodeResultat ->
            periodeResultat.vilkårResultater.forEach { vilkårResultat -> vilkårResultat.begrunnelse = begrunnelse }
        }
        return behandlingResultatRepository.save(aktivBehandlingResultat)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}