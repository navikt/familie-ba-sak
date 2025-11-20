package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
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
    fun hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
        statsborgerskap: List<GrStatsborgerskap>,
        person: Person,
        eldsteBarnsFødselsdato: LocalDate,
    ): List<GrArbeidsforhold> {
        val statsborgerskapGruppertPåLand =
            statsborgerskap.groupBy { statsborgerskap ->
                statsborgerskap.landkode
            }

        val medlesmkapTidslinjeForHvertLand =
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
            medlesmkapTidslinjeForHvertLand
                .fold(tomTidslinje<Medlemskap>()) { sterkesteMedlemskapTidslinje, medlemskapTidslinje ->
                    sterkesteMedlemskapTidslinje.kombinerMed(medlemskapTidslinje) { sterkesteMedlemskap, nyttMedlesmkap ->
                        listOfNotNull(sterkesteMedlemskap, nyttMedlesmkap).finnSterkesteMedlemskap()
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
