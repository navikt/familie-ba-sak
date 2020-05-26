package no.nav.familie.ba.sak.økonomi


data class OppdragIdForFagsystem(val personIdent: String, val behandlingsId: Long) {
    override fun toString(): String {
        return "OppdragIdForFagsystem(behandlingsId = $behandlingsId)"
    }
}