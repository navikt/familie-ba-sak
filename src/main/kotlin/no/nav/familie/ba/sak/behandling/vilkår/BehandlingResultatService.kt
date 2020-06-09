package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

    fun hentBehandlingResultatForBehandling(behandlingId: Long): List<BehandlingResultat> {
        return behandlingResultatRepository.finnBehandlingResultater(behandlingId=behandlingId)
    }


    fun oppdater(behandlingResultat: BehandlingResultat): BehandlingResultat {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer behandlingsresultat $behandlingResultat")
        return behandlingResultatRepository.saveAndFlush(behandlingResultat)
    }

    fun loggOpprettBehandlingsresultat(behandlingResultat: BehandlingResultat, behandling: Behandling) {
        val aktivBehandlingResultat = hentAktivForBehandling(behandling.id)
        val alleBehandlingsresultat = behandlingResultatRepository.finnBehandlingResultater(behandling.id)
        val forrigeBehandlingResultatSomIkkeErAutogenerert: BehandlingResultat? =
                if (alleBehandlingsresultat.size > 1)
                    aktivBehandlingResultat
                else null

        loggService.opprettVilkårsvurderingLogg(behandling,
                forrigeBehandlingResultatSomIkkeErAutogenerert, behandlingResultat)

    }

    fun lagreNyOgDeaktiverGammel(behandlingResultat: BehandlingResultat,
                                 loggHendelse: Boolean): BehandlingResultat {
        val aktivBehandlingResultat = hentAktivForBehandling(behandlingResultat.behandling.id)

        if (aktivBehandlingResultat != null) {
            behandlingResultatRepository.saveAndFlush(aktivBehandlingResultat.also { it.aktiv = false })
        }

        if(loggHendelse) {
            loggOpprettBehandlingsresultat(behandlingResultat=behandlingResultat,
                    behandling = behandlingResultat.behandling)
        }


        return behandlingResultatRepository.save(behandlingResultat)
    }

    fun lagreInitiert(behandlingResultat: BehandlingResultat): BehandlingResultat {
        val aktivBehandlingResultat = hentAktivForBehandling(behandlingResultat.behandling.id)
        if (aktivBehandlingResultat != null) {
            error("Det finnes allerede et aktivt behandlingsresultat for behandling ${behandlingResultat.behandling.id}")
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling resultat $behandlingResultat")
        return behandlingResultatRepository.save(behandlingResultat)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}