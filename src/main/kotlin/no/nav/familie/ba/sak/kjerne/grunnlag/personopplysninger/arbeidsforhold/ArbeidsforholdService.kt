package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArbeidsforholdService(
    private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient,
) {
    fun hentArbeidsforholdForFødselshendelser(
        person: Person,
        ansettelsesperiodeFom: LocalDate,
    ): List<GrArbeidsforhold> {
        val arbeidsforhold =
            systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(person.aktør.aktivFødselsnummer(), ansettelsesperiodeFom)

        return arbeidsforhold.map {
            val periode = DatoIntervallEntitet(it.ansettelsesperiode?.periode?.fom, it.ansettelsesperiode?.periode?.tom)
            val arbeidsgiverId =
                when (it.arbeidsgiver?.type) {
                    ArbeidsgiverType.Organisasjon -> it.arbeidsgiver.organisasjonsnummer
                    ArbeidsgiverType.Person -> it.arbeidsgiver.offentligIdent
                    else -> null
                }

            GrArbeidsforhold(
                periode = periode,
                arbeidsgiverType = it.arbeidsgiver?.type?.name,
                arbeidsgiverId = arbeidsgiverId,
                person = person,
            )
        }
    }
}
