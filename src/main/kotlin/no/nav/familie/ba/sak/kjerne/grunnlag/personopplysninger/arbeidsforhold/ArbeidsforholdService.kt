package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold.Companion.tilGrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.beskjærFraOgMed
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import java.time.LocalDate

@Service
class ArbeidsforholdService(
    private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient,
    private val integrasjonKlient: IntegrasjonKlient,
) {
    private val logger = LoggerFactory.getLogger(ArbeidsforholdService::class.java)

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
        cutOffFomDato: LocalDate,
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
                            listOf(
                                Periode(
                                    fom = it.gyldigPeriode?.fom,
                                    tom = it.gyldigPeriode?.tom,
                                    verdi = it.medlemskap,
                                ),
                            ).tilTidslinje()
                        }.kombiner { it.finnSterkesteMedlemskap() }
                }

        val sterkesteMedlemskapPerioder =
            medlemskapTidslinjeForHvertLand
                .kombiner { it.finnSterkesteMedlemskap() }
                .beskjærFraOgMed(cutOffFomDato)
                .tilPerioderIkkeNull()

        return sterkesteMedlemskapPerioder
            .filter { it.verdi == Medlemskap.EØS }
            .flatMap { medlemskapPeriode ->
                systemOnlyIntegrasjonKlient
                    .hentArbeidsforholdMedSystembruker(
                        ident = person.aktør.aktivFødselsnummer(),
                        ansettelsesperiodeFom = medlemskapPeriode.fom ?: cutOffFomDato,
                        ansettelsesperiodeTom = medlemskapPeriode.tom,
                    ).associateWith { arbeidsforhold ->
                        arbeidsforhold.arbeidsgiver?.organisasjonsnummer?.let { hentOrganisasjonMedFallback(it) }
                    }.map { (arbeidsforhold, organisasjon) ->
                        arbeidsforhold.tilGrArbeidsforhold(person, organisasjon)
                    }
            }
    }

    private fun hentOrganisasjonMedFallback(organisasjonsnummer: String): Organisasjon =
        try {
            integrasjonKlient.hentOrganisasjon(organisasjonsnummer)
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Fant ikke organisasjon $organisasjonsnummer i EREG, bruker 'Ukjent navn' som fallback.")
                Organisasjon(organisasjonsnummer = organisasjonsnummer, navn = "Ukjent navn")
            } else {
                throw e
            }
        }
}
