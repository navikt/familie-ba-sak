package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingService(
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val loggService: LoggService
) {

    fun hentBehandlingResultatTypeFraBehandling(behandling: Behandling): BehandlingResultatType {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
                               ?: return BehandlingResultatType.IKKE_VURDERT

        return vilkårsvurdering.samletResultat
    }

    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlingResultatForBehandling(behandlingId: Long): List<Vilkårsvurdering> {
        return vilkårsvurderingRepository.finnBehandlingResultater(behandlingId = behandlingId)
    }


    fun oppdater(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer vilkårsvurdering $vilkårsvurdering")
        return vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun loggOpprettBehandlingsresultat(aktivVilkårsvurdering: Vilkårsvurdering,
                                       nyttSamletBehandlingResultat: BehandlingResultatType,
                                       behandling: Behandling) {
        val alleBehandlingsresultat = vilkårsvurderingRepository.finnBehandlingResultater(behandling.id)
        val forrigeVilkårsvurderingSomIkkeErAutogenerert: Vilkårsvurdering? =
                alleBehandlingsresultat.sortedByDescending { it.opprettetTidspunkt }.firstOrNull { !it.aktiv }

        loggService.opprettVilkårsvurderingLogg(behandling,
                                                aktivVilkårsvurdering,
                                                nyttSamletBehandlingResultat,
                                                forrigeVilkårsvurderingSomIkkeErAutogenerert)
    }

    fun lagreNyOgDeaktiverGammel(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivBehandlingResultat = hentAktivForBehandling(vilkårsvurdering.behandling.id)

        if (aktivBehandlingResultat != null) {
            vilkårsvurderingRepository.saveAndFlush(aktivBehandlingResultat.also { it.aktiv = false })
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun lagreInitielt(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivBehandlingResultat = hentAktivForBehandling(vilkårsvurdering.behandling.id)
        if (aktivBehandlingResultat != null) {
            error("Det finnes allerede et aktivt vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun settBehandlingResultatTilHenlagt(behandling: Behandling, behandlingResultatType: BehandlingResultatType) {
        val vilkårsvurdering =
                hentAktivForBehandling(behandling.id) ?: lagreInitielt(Vilkårsvurdering(behandling = behandling))
        vilkårsvurdering.oppdaterSamletResultat(behandlingResultatType)
        oppdater(vilkårsvurdering)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}