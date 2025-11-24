package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold.Companion.tilGrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArbeidsforholdService(
    private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient,
) {
    fun hentArbeidsforhold(person: Person): List<GrArbeidsforhold> {
        val arbeidsforholdForSisteFemÅr = systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(person.aktør.aktivFødselsnummer(), LocalDate.now().minusYears(5))

        return arbeidsforholdForSisteFemÅr.map {
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

    fun hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
        statsborgerskap: List<GrStatsborgerskap>,
        person: Person,
        eldsteBarnsFødselsdato: LocalDate,
    ): List<GrArbeidsforhold> {
        val statsborgerskapGruppertPåLand =
            statsborgerskap.groupBy { statsborgerskap ->
                statsborgerskap.landkode
            }

        val medlemskapTidslinjeForHvertLand =
            statsborgerskapGruppertPåLand
                .map { (_, statsborgerskap) ->
                    statsborgerskap
                        .map {
                            Periode(
                                fom = it.gyldigPeriode?.fom,
                                tom = it.gyldigPeriode?.tom,
                                verdi = it.medlemskap,
                            )
                        }.tilTidslinje()
                }
        val sterkesteMedlemskapPerioder =
            medlemskapTidslinjeForHvertLand
                .fold(tomTidslinje<Medlemskap>()) { sterkesteMedlemskapTidslinje, medlemskapTidslinje ->
                    sterkesteMedlemskapTidslinje.kombinerMed(medlemskapTidslinje) { sterkesteMedlemskap, nyttMedlemskap ->
                        listOfNotNull(sterkesteMedlemskap, nyttMedlemskap).finnSterkesteMedlemskap()
                    }
                }.tilPerioderIkkeNull()

        return sterkesteMedlemskapPerioder
            .filter { it.verdi == Medlemskap.EØS }
            .flatMap { medlemskapPeriode ->
                val arbeidsforhold =
                    systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(
                        ident = person.aktør.aktivFødselsnummer(),
                        ansettelsesperiodeFom = medlemskapPeriode.fom ?: eldsteBarnsFødselsdato,
                        ansettelsesperiodeTom = medlemskapPeriode.tom,
                    )
                arbeidsforhold.map { it.tilGrArbeidsforhold(person) }
            }
    }
}
