package no.nav.familie.ba.sak.opplysningsplikt

import org.springframework.stereotype.Service

@Service
class OpplysningspliktService(
        private val opplysningspliktRepository: OpplysningspliktRepository
) {

    fun hentOpplysningsplikt(behandlingId: Long): Opplysningsplikt? = opplysningspliktRepository.findByBehandlingId(behandlingId)

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
}