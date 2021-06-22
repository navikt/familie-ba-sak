package no.nav.familie.ba.sak.kjerne.automatiskvurdering

data class FiltreringIAutomatiskBehandling(
    private val morFnr: Boolean,
    private val barnFnr: Boolean,
    private val morLever: Boolean,
    private val barnLever: Boolean,
    private val barnMindreEnnFemMnd: Boolean,
    private val morOver18: Boolean,
    private val morHarIkkeVerge: Boolean
) {
    fun søkerPassererFiltering(): Boolean {
        return (morLever && barnLever && barnMindreEnnFemMnd && morOver18 && morHarIkkeVerge && morFnr && barnFnr)
    }

    fun hentBegrunnelseFraFiltrering(): String {
        if (!morFnr) {
            return "Fødselshendelse: Mor ikke gyldig fødselsnummer"
        }
        if (!barnFnr) {
            return "Fødselshendelse: Barnet ikke gyldig fødselsnummer"
        }
        if (!morLever) {
            return "Fødselshendelse: Registrert dødsdato på mor"
        }
        if (!barnLever) {
            return "Fødselshendelse: Registrert dødsdato på barnet"
        }
        if (!barnMindreEnnFemMnd) {
            return "Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom"
        }
        if (!morOver18) {
            return "Fødselshendelse: Mor under 18 år"
        }
        if (!morHarIkkeVerge) {
            return "Fødselshendelse: Mor er umyndig"
        }
        return "Saken skal behandles i BA-SAK"
    }
}