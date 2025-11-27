package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene

import java.time.LocalDate

class Arbeidsforhold(
    val arbeidsgiver: Arbeidsgiver? = null,
    val ansettelsesperiode: Ansettelsesperiode? = null,
)

class Arbeidsgiver(
    val type: ArbeidsgiverType? = null,
    val organisasjonsnummer: String? = null,
    val offentligIdent: String? = null,
)

class Ansettelsesperiode(
    val periode: Periode? = null,
)

class Periode(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

enum class ArbeidsgiverType {
    Organisasjon,
    Person,
}

class ArbeidsforholdRequest(
    val personIdent: String,
    // Trengs i requesten, så overser klaging på at den aldri blir brukt
    @Suppress("UNUSED")
    val ansettelsesperiodeFom: LocalDate,
    val ansettelsesperiodeTom: LocalDate? = null,
)
