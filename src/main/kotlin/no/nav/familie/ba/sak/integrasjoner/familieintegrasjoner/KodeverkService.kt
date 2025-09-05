package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import org.springframework.stereotype.Service

@Service
class KodeverkService(
    private val integrasjonClient: IntegrasjonClient,
) {
    fun hentPoststed(postnummer: String?): String? =
        integrasjonClient
            .hentPoststeder()
            .betydninger[postnummer]
            ?.firstOrNull()
            ?.beskrivelser[KodeverkSpråk.BOKMÅL.kode]
            ?.term
            ?.storForbokstav()
}
