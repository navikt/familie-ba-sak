package no.nav.familie.ba.sak.opplysningsplikt

import no.nav.familie.ba.sak.behandling.restDomene.RestOpplysningsplikt
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.logg.LoggService
import org.springframework.stereotype.Service

@Service
class OpplysningspliktService(
        private val opplysningspliktRepository: OpplysningspliktRepository,
        private val loggService: LoggService
) {

    fun hent(behandlingId: Long): Opplysningsplikt? = opplysningspliktRepository.findByBehandlingId(behandlingId)

    fun lagreBlankOpplysningsplikt(behandlingId: Long) {
        val lagretOpplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandlingId)
        if (lagretOpplysningsplikt == null) {
            opplysningspliktRepository.save(Opplysningsplikt(behandlingId = behandlingId,
                                                             status = OpplysningspliktStatus.IKKE_SATT))
        } else {
            opplysningspliktRepository.saveAndFlush(lagretOpplysningsplikt.also {
                it.status = OpplysningspliktStatus.IKKE_SATT
                it.begrunnelse = null
            })
        }
    }

    fun oppdaterOpplysningsplikt(behandlingId: Long, restOpplysningsplikt: RestOpplysningsplikt) {
        val lagretOpplysningsplikt = opplysningspliktRepository.findByBehandlingId(behandlingId)
                                     ?: error("Kunne ikke oppdatere opplysningsplikt fordi opplysningsplikt mangler på behandling")
        if (restOpplysningsplikt.status != null) {
            opplysningspliktRepository.saveAndFlush(lagretOpplysningsplikt.also {
                it.status = restOpplysningsplikt.status
                it.begrunnelse = restOpplysningsplikt.begrunnelse
            })
            loggService.opprettOpplysningspliktEndret(behandlingId = behandlingId,
                                                      endring = true,
                                                      status = restOpplysningsplikt.status,
                                                      begrunnelse = restOpplysningsplikt.begrunnelse)

        } else {
            throw Feil("Status må finnes når opplysningsplikt skal oppdateres",
                       "Finner ikke ny status ved oppdatering av opplysningsplikt")
        }
    }
}