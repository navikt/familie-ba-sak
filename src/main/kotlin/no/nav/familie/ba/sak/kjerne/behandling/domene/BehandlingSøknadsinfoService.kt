package no.nav.familie.ba.sak.kjerne.behandling.domene

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BehandlingSøknadsinfoService(
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository
) {

    @Transactional
    fun lagreNedSøknadMottattDato(mottattDato: LocalDate, behandling: Behandling) {
        val behandlingSøknadsinfo = BehandlingSøknadsinfo(
            behandling = behandling,
            mottattDato = mottattDato.atStartOfDay()
        )
        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)
    }

    fun hentSøknadMottattDato(behandlingId: Long): LocalDateTime? {
        return behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId)?.mottattDato
    }
}
