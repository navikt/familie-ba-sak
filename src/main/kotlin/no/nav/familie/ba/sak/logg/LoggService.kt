package no.nav.familie.ba.sak.logg

import org.springframework.stereotype.Service

@Service
class LoggService(
        private val loggRepository: LoggRepository
) {

    fun lagre(logg: Logg) {
        loggRepository.save(logg)
    }

    fun hentLoggForBehandling(behandlingId: Long): List<Logg> {
        return loggRepository.hentLoggForBehandling(behandlingId)
    }
}