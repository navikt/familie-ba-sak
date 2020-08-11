package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.ArbeidsgiverType
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArbeidsforholdService(private val integrasjonClient: IntegrasjonClient) {
    fun hentArbeidsforhold(ident: Ident, person: Person): List<GrArbeidsforhold> {

        val arbeidsforholdForSisteFemÅr = integrasjonClient.hentArbeidsforhold(ident.ident, LocalDate.now().minusYears(5))

        return arbeidsforholdForSisteFemÅr.map {
            val periode = DatoIntervallEntitet(it.ansettelsesperiode?.periode?.fom, it.ansettelsesperiode?.periode?.tom)
            val arbeidsgiverId = when (it.arbeidsgiver?.type) {
                ArbeidsgiverType.Organisasjon -> it.arbeidsgiver.organisasjonsnummer
                ArbeidsgiverType.Person -> it.arbeidsgiver.offentligIdent
                else -> null
            }

            GrArbeidsforhold(periode = periode, arbeidsgiverType = it.arbeidsgiver?.type?.name, arbeidsgiverId = arbeidsgiverId, person = person)
        }
    }
}