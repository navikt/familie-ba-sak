package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype.ORDINÆR
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype.UTVIDET
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class BehandlingSøknadsinfoService(
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
) {
    @Transactional
    fun lagreSøknadsinfo(
        mottattDato: LocalDate,
        søknadsinfo: Søknadsinfo?,
        behandling: Behandling,
    ) {
        val behandlingSøknadsinfo =
            BehandlingSøknadsinfo(
                behandling = behandling,
                mottattDato = mottattDato.atStartOfDay(),
                journalpostId = søknadsinfo?.journalpostId,
                brevkode = søknadsinfo?.brevkode,
                erDigital = søknadsinfo?.erDigital,
            )
        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)
    }

    fun hentSøknadMottattDato(behandlingId: Long): LocalDateTime? = behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId).minOfOrNull { it.mottattDato }

    fun hentJournalpostId(behandlingId: Long): String? = behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId).firstOrNull()?.journalpostId

    fun finnDigitalSøknad(behandlingId: Long): BehandlingSøknadsinfo? = behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId).firstOrNull { it.erDigital == true }
}
