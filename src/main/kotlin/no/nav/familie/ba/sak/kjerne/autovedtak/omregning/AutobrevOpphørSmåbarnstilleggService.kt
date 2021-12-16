package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutobrevOpphørSmåbarnstilleggService {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(behandlingId: Long) {
    }
}
