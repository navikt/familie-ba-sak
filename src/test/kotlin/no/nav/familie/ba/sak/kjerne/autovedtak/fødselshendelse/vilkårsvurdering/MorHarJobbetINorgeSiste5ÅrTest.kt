package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MorHarJobbetINorgeSiste5ÅrTest {

    private fun lagSøkerMedArbeidsforhold(perioder: List<DatoIntervallEntitet>?): Person {
        val fnr = randomFnr()
        return Person(
            aktør = tilAktør(fnr),
            type = PersonType.SØKER,
            personopplysningGrunnlag = PersonopplysningGrunnlag(0, 0, mutableSetOf(), true),
            fødselsdato = LocalDate.of(1991, 1, 1),
            navn = "navn",
            kjønn = Kjønn.KVINNE,
            bostedsadresser = mutableListOf(),
        ).also { person ->
            person.arbeidsforhold = perioder?.map {
                GrArbeidsforhold(
                    periode = it,
                    person = person,
                    arbeidsgiverId = null,
                    arbeidsgiverType = null
                )
            }?.toMutableList() ?: mutableListOf()
            person.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.GIFT, person = person))
        }
    }

    @Test
    fun `skal returnere usant dersom arbeidsforhold er null eller listen av arbeidsforhold er tom`() {
        assertThat(morHarJobbetINorgeSiste5År(lagSøkerMedArbeidsforhold(null))).isFalse
        assertThat(morHarJobbetINorgeSiste5År(lagSøkerMedArbeidsforhold(emptyList()))).isFalse
    }

    @Test
    fun `90 dagers arbeidsfravær i starten av 5-års-perioden skal godkjennes`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(90), tom = null)
                    )
                )
            )
        ).isTrue
    }

    @Test
    fun `91 dagers arbeidsfravær i starten av 5-års-perioden skal avslås`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(91), tom = null)
                    )
                )
            )
        ).isFalse
    }

    @Test
    fun `90 dagers arbeidsfravær i slutten av 5-års-perioden skal godkjennes`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(90))
                    )
                )
            )
        ).isTrue
    }

    @Test
    fun `91 dagers arbeidsfravær i slutten av 5-års-perioden skal avslås`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(91))
                    )
                )
            )
        ).isFalse
    }

    @Test
    fun `et mellomrom på 90 dager (eller mindre) mellom to arbeidsperioder skal godkjennes`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusDays(2), tom = LocalDate.now()),
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(93))
                    )
                )
            )
        ).isTrue
    }

    @Test
    fun `et mellomrom på 91 dager (eller mer) mellom to arbeidsperioder skal avslås`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusDays(2), tom = LocalDate.now()),
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(94))
                    )
                )
            )
        ).isFalse
    }

    @Test
    fun `arbeidsforhold som startet fra før 5 år siden skal tas med i beregningen`() {
        assertThat(
            morHarJobbetINorgeSiste5År(
                lagSøkerMedArbeidsforhold(
                    listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(8))
                    )
                )
            )
        ).isTrue
    }
}
