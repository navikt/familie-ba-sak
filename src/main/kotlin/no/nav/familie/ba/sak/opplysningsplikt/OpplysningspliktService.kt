package no.nav.familie.ba.sak.opplysningsplikt

import org.springframework.stereotype.Service

@Service
class OpplysningspliktService(
        private val opplysningspliktRepository: OpplysningspliktRepository
) {

    fun hentOpplysningsplikt(behandlingId: Long): Opplysningsplikt? =
            opplysningspliktRepository.findByBehandlingId(behandlingId)
}