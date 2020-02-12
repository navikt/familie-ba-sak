package no.nav.familie.ba.sak.økonomi


data class OppdragId(val personIdent: String, val behandlingsId: Long) {
    override fun toString(): String { return "OppdragId(behandlingsId = $behandlingsId)"}
}