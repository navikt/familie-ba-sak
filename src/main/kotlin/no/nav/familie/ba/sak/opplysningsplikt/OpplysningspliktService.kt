package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class OpplysningspliktService(
        private val opplysningspliktRepository: OpplysningspliktRepository,
        private val loggService: LoggService
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
}