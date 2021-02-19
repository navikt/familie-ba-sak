package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingService(
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {

    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlingResultatForBehandling(behandlingId: Long): List<Vilkårsvurdering> {
        return vilkårsvurderingRepository.finnBehandlingResultater(behandlingId = behandlingId)
    }

    fun finnPersonerMedEksplisittAvslagPåBehandling(behandlingId: Long): List<String> {
        val eksplisistteAvslagPåBehandling = hentEksplisitteAvslagPåBehandling(behandlingId)
        return eksplisistteAvslagPåBehandling.map { it.personResultat!!.personIdent }.toSet().toList()
    }

    private fun hentEksplisitteAvslagPåBehandling(behandlingId: Long): List<VilkårResultat> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        return vilkårsvurdering?.personResultater?.flatMap { it.vilkårResultater }
                       ?.filter { it.erEksplisittAvslagPåSøknad ?: false } ?: emptyList()
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