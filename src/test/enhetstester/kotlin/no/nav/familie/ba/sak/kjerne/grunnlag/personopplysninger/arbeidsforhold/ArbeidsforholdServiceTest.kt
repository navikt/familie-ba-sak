package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsgiver
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.ArbeidsgiverType
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidsforholdServiceTest {
    val systemOnlyIntegrasjonKlient = mockk<SystemOnlyIntegrasjonKlient>()
    val integrasjonKlient = mockk<IntegrasjonKlient>()
    val arbeidsforholdService = ArbeidsforholdService(systemOnlyIntegrasjonKlient, integrasjonKlient)

    val person = lagPerson()
    val nåværendeArbeidsforhold =
        Arbeidsforhold(
            arbeidsgiver = Arbeidsgiver(organisasjonsnummer = "123456789", type = ArbeidsgiverType.Organisasjon),
            ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(5))),
        )
    val eldreArbeidsforhold =
        Arbeidsforhold(
            arbeidsgiver = Arbeidsgiver(organisasjonsnummer = "234567891", type = ArbeidsgiverType.Organisasjon),
            ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(8), LocalDate.now().minusYears(5))),
        )
    val privatArbeidsforhold =
        Arbeidsforhold(
            arbeidsgiver = Arbeidsgiver(offentligIdent = "12345678910", type = ArbeidsgiverType.Person),
            ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(3))),
        )

    @BeforeEach
    fun setup() {
        every {
            systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(
                ident = person.aktør.aktivFødselsnummer(),
                ansettelsesperiodeFom = any(),
                ansettelsesperiodeTom = any(),
            )
        } answers {
            val ansettelsesperiodeFom = secondArg<LocalDate?>()

            if (ansettelsesperiodeFom == null) return@answers listOf()

            if (ansettelsesperiodeFom >= LocalDate.now().minusYears(5)) {
                listOf(nåværendeArbeidsforhold)
            } else {
                listOf(nåværendeArbeidsforhold, eldreArbeidsforhold)
            }
        }
        every { integrasjonKlient.hentOrganisasjon(nåværendeArbeidsforhold.arbeidsgiver!!.organisasjonsnummer!!) } returns
            Organisasjon(
                organisasjonsnummer = nåværendeArbeidsforhold.arbeidsgiver!!.organisasjonsnummer!!,
                navn = "Nåværende AS",
            )
        every { integrasjonKlient.hentOrganisasjon(eldreArbeidsforhold.arbeidsgiver!!.organisasjonsnummer!!) } returns
            Organisasjon(
                organisasjonsnummer = eldreArbeidsforhold.arbeidsgiver!!.organisasjonsnummer!!,
                navn = "Eldre AS",
            )
    }

    @Test
    fun `lager arbeidsforhold for perioder der det sterkeste medlemskapet er i EØS`() {
        // Arrange
        val statsborgerskap =
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1), tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                    landkode = "AFG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = person,
                ),
            )

        // Act
        val arbeidsforhold =
            arbeidsforholdService.hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
                statsborgerskap,
                person,
                LocalDate.now().minusYears(20),
            )

        // Assert
        assertThat(arbeidsforhold).hasSize(1)
        assertThat(arbeidsforhold.single().arbeidsgiverType).isEqualTo(ArbeidsgiverType.Organisasjon.name)
        assertThat(arbeidsforhold.single().arbeidsgiverId).isEqualTo(nåværendeArbeidsforhold.arbeidsgiver?.organisasjonsnummer)
        assertThat(arbeidsforhold.single().organisasjonNavn).isEqualTo("Nåværende AS")
        val ansettelsesperiode = nåværendeArbeidsforhold.ansettelsesperiode?.periode!!
        assertThat(arbeidsforhold.single().periode?.fom).isEqualTo(ansettelsesperiode.fom)
        assertThat(arbeidsforhold.single().periode?.tom).isEqualTo(ansettelsesperiode.tom)
    }

    @Test
    fun `lager ikke arbeidsforhold hvis sterkeste medlemskap ikke er EØS`() {
        // Arrange
        val person = lagPerson()

        val statsborgerskap =
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1), tom = null),
                    landkode = "SWE",
                    medlemskap = Medlemskap.NORDEN,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                    landkode = "AFG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = person,
                ),
            )

        // Act
        val arbeidsforhold =
            arbeidsforholdService.hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
                statsborgerskap,
                person,
                LocalDate.now().minusYears(20),
            )

        // Assert
        assertThat(arbeidsforhold).isEmpty()
    }

    @Test
    fun `får ikke inn eldre arbeidsforhold enn eldste barns fødselsdato når statsborgerskap ikke har fom-dato`() {
        // Arrange
        val statsborgerskap =
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = null, tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                    landkode = "AFG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = person,
                ),
            )

        // Act
        val arbeidsforhold =
            arbeidsforholdService.hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
                statsborgerskap,
                person,
                LocalDate.now().minusYears(4),
            )

        // Assert
        assertThat(arbeidsforhold).hasSize(1)
        assertThat(arbeidsforhold.single().arbeidsgiverType).isEqualTo(ArbeidsgiverType.Organisasjon.name)
        assertThat(arbeidsforhold.single().arbeidsgiverId).isEqualTo(nåværendeArbeidsforhold.arbeidsgiver?.organisasjonsnummer)
        assertThat(arbeidsforhold.single().organisasjonNavn).isEqualTo("Nåværende AS")
        val ansettelsesperiode = nåværendeArbeidsforhold.ansettelsesperiode?.periode!!
        assertThat(arbeidsforhold.single().periode?.fom).isEqualTo(ansettelsesperiode.fom)
        assertThat(arbeidsforhold.single().periode?.tom).isEqualTo(ansettelsesperiode.tom)
    }

    @Test
    fun `flere statsborgerskap uten tom og fom på samme land skaper ikke tidslinjeproblemer`() {
        // Arrange
        val statsborgerskap =
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = null, tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = null, tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(20), tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                    landkode = "AFG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = person,
                ),
            )

        // Act
        val arbeidsforhold =
            arbeidsforholdService.hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
                statsborgerskap,
                person,
                LocalDate.now().minusYears(4),
            )

        // Assert
        assertThat(arbeidsforhold).`as`("Forventer å finne ett arbeidsforhold").hasSize(1)
        assertThat(arbeidsforhold.single().arbeidsgiverType).isEqualTo(ArbeidsgiverType.Organisasjon.name)
        assertThat(arbeidsforhold.single().arbeidsgiverId).isEqualTo(nåværendeArbeidsforhold.arbeidsgiver?.organisasjonsnummer)
        assertThat(arbeidsforhold.single().organisasjonNavn).isEqualTo("Nåværende AS")
        val ansettelsesperiode = nåværendeArbeidsforhold.ansettelsesperiode?.periode!!
        assertThat(arbeidsforhold.single().periode?.fom).isEqualTo(ansettelsesperiode.fom)
        assertThat(arbeidsforhold.single().periode?.tom).isEqualTo(ansettelsesperiode.tom)
    }

    @Test
    fun `lagrer riktig data for privat arbeidsforhold`() {
        // Arrange
        every { systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(person.aktør.aktivFødselsnummer(), any(), any()) } returns listOf(privatArbeidsforhold)

        val statsborgerskap =
            listOf(
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1), tom = null),
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = person,
                ),
                GrStatsborgerskap(
                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(10), tom = null),
                    landkode = "AFG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = person,
                ),
            )

        // Act
        val arbeidsforhold =
            arbeidsforholdService.hentArbeidsforholdPerioderMedSterkesteMedlemskapIEØS(
                statsborgerskap,
                person,
                LocalDate.now().minusYears(20),
            )

        // Assert
        assertThat(arbeidsforhold).hasSize(1)
        assertThat(arbeidsforhold.single().arbeidsgiverType).isEqualTo(ArbeidsgiverType.Person.name)
        assertThat(arbeidsforhold.single().arbeidsgiverId).isEqualTo(privatArbeidsforhold.arbeidsgiver?.offentligIdent)
        assertThat(arbeidsforhold.single().organisasjonNavn).isNull()
        val ansettelsesperiode = privatArbeidsforhold.ansettelsesperiode?.periode!!
        assertThat(arbeidsforhold.single().periode?.fom).isEqualTo(ansettelsesperiode.fom)
        assertThat(arbeidsforhold.single().periode?.tom).isEqualTo(ansettelsesperiode.tom)
    }
}
