package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.behandling.restDomene.RestOpplysningsplikt
import org.springframework.stereotype.Service

@Service
class OpplysningspliktService(
        private val opplysningspliktRepository: OpplysningspliktRepository
) {


    fun lagreBlankOpplysningsplikt(behandlingId: Long) {
        val lagretOpplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandlingId)
        if (lagretOpplysningsplikt == null) {
            opplysningspliktRepository.save(Opplysningsplikt(behandlingId = behandlingId))
        } else {
            opplysningspliktRepository.saveAndFlush(lagretOpplysningsplikt.also {
                it.status = null
                it.begrunnelse = null
            })
        }
    }

    fun oppdaterOpplysningsplikt(behandlingId: Long, restOpplysningsplikt: RestOpplysningsplikt) {
        val lagretOpplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandlingId) ?: error("Kunne ikke oppdatere opplysningsplikt fordi opplysningsplikt mangler på behandling")
        opplysningspliktRepository.saveAndFlush(lagretOpplysningsplikt.also {
            it.status = restOpplysningsplikt.status
            it.begrunnelse = restOpplysningsplikt.begrunnelse
        })
    }
}