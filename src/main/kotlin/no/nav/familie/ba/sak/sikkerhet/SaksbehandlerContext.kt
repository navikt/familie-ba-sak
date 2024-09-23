package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SaksbehandlerContext(
    @Value("\${rolle.kode6}")
    private val kode6GruppeId: String,
    @Value("\${rolle.kode7}")
    private val kode7GruppeId: String,
) {
    fun hentSaksbehandlerSignaturTilBrev(): String {
        val grupper = SikkerhetContext.hentGrupper()

        return if (grupper.contains(kode6GruppeId)) {
            ""
        } else {
            SikkerhetContext.hentSaksbehandlerNavn()
        }
    }

    fun harTilgang(adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING): Boolean {
        val grupper = SikkerhetContext.hentGrupper()
        return when (adressebeskyttelsegradering) {
            ADRESSEBESKYTTELSEGRADERING.FORTROLIG -> grupper.contains(kode7GruppeId)
            ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG, ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND -> grupper.contains(kode6GruppeId)
            ADRESSEBESKYTTELSEGRADERING.UGRADERT -> true
        }
    }
}
