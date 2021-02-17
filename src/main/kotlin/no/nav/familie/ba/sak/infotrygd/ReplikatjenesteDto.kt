package no.nav.familie.ba.sak.infotrygd

import java.time.LocalDate


data class InfotrygdSøkRequest(val brukere: List<String>,
                               val barn: List<String>? = null)

data class InfotrygdSøkResponse<T>(
        val bruker: List<T>,
        val barn: List<T>,
)

data class InfotrygdTreffResponse(val ingenTreff: Boolean)

data class StønadDto(
        val stønadId: Long,
        val sakNr: String? = null,
        val opphørtFom: String? = null,
        val opphørsgrunn: String? = null,
)

data class SakDto(
        val saksnr: String? = null,
        val saksblokk: String? = null,
        val regDato: LocalDate? = null,
        val mottattdato: LocalDate? = null,
        val kapittelnr: String? = null,
        val valg: String? = null,
        val undervalg: String? = null,
        val type: String? = null,
        val nivå: String? = null,
        val resultat: String? = null,
        val vedtaksdato: LocalDate? = null,
        val iverksattdato: LocalDate? = null,
        val stønadList: List<StønadDto> = emptyList(),
        val årsakskode: String? = null,
        val behenEnhet: String? = null,
        val regAvEnhet: String? = null,
        val status: String,
)

enum class Opphørsgrunn(val kode: String) {
    MIGRERT("5")
}

enum class StatusKode(val beskrivelse: String) {
    IP("Saksbehandlingen kan starte med Statuskode IP (Ikke påbegynt). Da er det kun registrert en sakslinje uten at vedtaksbehandling er startet."),
    UB("Saksbehandling startet - når sak med status UB - Under Behandling - lagres, rapporteres hendelsen BehandlingOpprettet"),
    SG("Saksbehandler 1 har fullført og sendt til saksbehandler 2 for godkjenning"),
    UK("Underkjent av saksbehandler 2 med retur til saksbehandler 1"),
    FB("FerdigBehandlet"),
    FI("ferdig iverksatt"),
    RF("returnert feilsendt"),
    RM("returnert midlertidig"),
    RT("returnert til"),
    ST("sendt til"),
    VD("videresendt Direktoratet"),
    VI("venter på iverksetting"),
    VT("videresendt Trygderetten"),
}