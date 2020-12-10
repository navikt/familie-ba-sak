package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingService(
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val loggService: LoggService
) {

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

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}