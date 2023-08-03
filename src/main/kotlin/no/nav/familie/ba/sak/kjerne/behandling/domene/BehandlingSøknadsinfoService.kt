package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BehandlingSøknadsinfoService(
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
) {

    @Transactional
    fun lagreNedSøknadsinfo(mottattDato: LocalDate, søknadsinfo: Søknadsinfo?, behandling: Behandling) {
        val behandlingSøknadsinfo = BehandlingSøknadsinfo(
            behandling = behandling,
            mottattDato = mottattDato.atStartOfDay(),
            journalpostId = søknadsinfo?.journalpostId,
            brevkode = søknadsinfo?.brevkode,
            erDigital = søknadsinfo?.erDigital,
        )
        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)
    }

    fun hentSøknadMottattDato(behandlingId: Long): LocalDateTime? {
        return behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId).minByOrNull { it.mottattDato }?.mottattDato
    }
}
