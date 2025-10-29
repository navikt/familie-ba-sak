package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import org.springframework.stereotype.Service

@Service
class KodeverkService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentLand(landkode: String): String = integrasjonKlient.hentLand(landkode)

    fun hentPoststed(postnummer: String?): String? =
        integrasjonKlient
            .hentPoststeder()
            .betydninger[postnummer]
            ?.firstOrNull()
            ?.beskrivelser[KodeverkSpråk.BOKMÅL.kode]
            ?.term
            ?.storForbokstav()

    fun henteEøsMedlemskapsPerioderForValgtLand(land: String) = integrasjonKlient.hentAlleEØSLand().betydninger[land] ?: emptyList()

    fun hentLandkoderISO2() = integrasjonKlient.hentLandkoderISO2()
}
