package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class FiltreringIAutomatiskBehandling(
    private val morFnr: Boolean,
    private val barnFnr: Boolean,
    private val morLever: Boolean,
    private val barnLever: Boolean,
    private val barnMindreEnnFemMndMellomrom: Boolean,
    private val morOver18: Boolean,
    private val morHarIkkeVerge: Boolean
) {
    fun søkerPassererFiltering(): Boolean {
        return (morLever && barnLever && barnMindreEnnFemMndMellomrom && morOver18 && morHarIkkeVerge && morFnr && barnFnr)
    }

    fun hentBegrunnelseFraFiltrering(): String {
        return when {
            !morFnr -> "Fødselshendelse: Mor ikke gyldig fødselsnummer"
            !barnFnr -> "Fødselshendelse: Barnet ikke gyldig fødselsnummer"
            !morLever -> "Fødselshendelse: Registrert dødsdato på mor"
            !barnLever -> "Fødselshendelse: Registrert dødsdato på barnet"
            !barnMindreEnnFemMndMellomrom -> "Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom"
            !morOver18 -> "Fødselshendelse: Mor under 18 år"
            !morHarIkkeVerge -> "Fødselshendelse: Mor er umyndig"
            else -> "Saken skal behandles i BA-SAK"
        }
    }
}