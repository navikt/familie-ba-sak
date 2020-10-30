package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.HenleggÅrsak
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

    fun hentBehandlingResultatTypeFraBehandling(behandling: Behandling): BehandlingResultatType {
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)
                                 ?: return BehandlingResultatType.IKKE_VURDERT

        return behandlingResultat.samletResultat
    }

    fun hentAktivForBehandling(behandlingId: Long): BehandlingResultat? {
        return behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlingResultatForBehandling(behandlingId: Long): List<BehandlingResultat> {
        return behandlingResultatRepository.finnBehandlingResultater(behandlingId = behandlingId)
    }


    fun oppdater(behandlingResultat: BehandlingResultat): BehandlingResultat {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer behandlingsresultat $behandlingResultat")
        return behandlingResultatRepository.saveAndFlush(behandlingResultat)
    }

    fun loggOpprettBehandlingsresultat(aktivBehandlingResultat: BehandlingResultat,
                                       nyttSamletBehandlingResultat: BehandlingResultatType,
                                       behandling: Behandling) {
        val alleBehandlingsresultat = behandlingResultatRepository.finnBehandlingResultater(behandling.id)
        val forrigeBehandlingResultatSomIkkeErAutogenerert: BehandlingResultat? =
                alleBehandlingsresultat.sortedByDescending { it.opprettetTidspunkt }.firstOrNull { !it.aktiv }

        loggService.opprettVilkårsvurderingLogg(behandling,
                                                aktivBehandlingResultat,
                                                nyttSamletBehandlingResultat,
                                                forrigeBehandlingResultatSomIkkeErAutogenerert)
    }

    fun lagreNyOgDeaktiverGammel(behandlingResultat: BehandlingResultat): BehandlingResultat {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandlingsresultat $behandlingResultat")

        val aktivBehandlingResultat = hentAktivForBehandling(behandlingResultat.behandling.id)

        if (aktivBehandlingResultat != null) {
            behandlingResultatRepository.saveAndFlush(aktivBehandlingResultat.also { it.aktiv = false })
        }

        return behandlingResultatRepository.save(behandlingResultat)
    }

    fun lagreInitielt(behandlingResultat: BehandlingResultat): BehandlingResultat {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandlingsresultat $behandlingResultat")

        val aktivBehandlingResultat = hentAktivForBehandling(behandlingResultat.behandling.id)
        if (aktivBehandlingResultat != null) {
            error("Det finnes allerede et aktivt behandlingsresultat for behandling ${behandlingResultat.behandling.id}")
        }

        return behandlingResultatRepository.save(behandlingResultat)
    }

    fun settBehandlingResultatTilHenlagt(behandling: Behandling, behandlingResultatType: BehandlingResultatType) {
        val behandlingsresultat = hentAktivForBehandling(behandling.id) ?:
                                  lagreInitielt(BehandlingResultat(behandling = behandling))
        behandlingsresultat.samletResultat = behandlingResultatType
        oppdater(behandlingsresultat)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}