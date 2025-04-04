package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternBehandlingRelasjonService(
    private val eksternBehandlingRelasjonRepository: EksternBehandlingRelasjonRepository,
) {
    @Transactional
    fun lagreEksternBehandlingRelasjon(eksternBehandlingRelasjon: EksternBehandlingRelasjon): EksternBehandlingRelasjon = eksternBehandlingRelasjonRepository.save(eksternBehandlingRelasjon)

    fun finnEksternBehandlingRelasjon(
        behandlingId: Long,
        fagsystem: EksternBehandlingRelasjon.Fagsystem,
    ): EksternBehandlingRelasjon? {
        val eksternBehandlingRelasjoner =
            eksternBehandlingRelasjonRepository
                .findAllByInternBehandlingId(behandlingId)
                .filter { it.eksternBehandlingFagsystem == fagsystem }
        if (eksternBehandlingRelasjoner.size > 1) {
            throw Feil("Forventet maks 1 ekstern behandling relasjon av type $fagsystem for behandling $behandlingId")
        }
        return eksternBehandlingRelasjoner.singleOrNull()
    }
}
