package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class ReglerTest {

    private fun lagSøkerMedArbeidsforhold(perioder: List<DatoIntervallEntitet>?): Person {
        return Person(
                aktørId = randomAktørId(),
                personIdent = PersonIdent(randomFnr()),
                type = PersonType.SØKER,
                personopplysningGrunnlag = PersonopplysningGrunnlag(0, 0, mutableSetOf(), true),
                fødselsdato = LocalDate.of(1991, 1, 1),
                navn = "navn",
                kjønn = Kjønn.KVINNE,
                bostedsadresse = null,
                sivilstand = SIVILSTAND.GIFT
        ).also { person ->
            person.arbeidsforhold = if (perioder == null) null else perioder.map {
                GrArbeidsforhold(
                        periode = it,
                        person = person,
                        arbeidsgiverId = null,
                        arbeidsgiverType = null
                )
            }
        }
    }

    private fun lagSøkerMedBostedsadresseperioder(perioder: List<DatoIntervallEntitet>?): Person {
        return Person(
                aktørId = randomAktørId(),
                personIdent = PersonIdent(randomFnr()),
                type = PersonType.SØKER,
                personopplysningGrunnlag = PersonopplysningGrunnlag(0, 0, mutableSetOf(), true),
                fødselsdato = LocalDate.of(1991, 1, 1),
                navn = "navn",
                kjønn = Kjønn.KVINNE,
                bostedsadresse = null,
                sivilstand = SIVILSTAND.GIFT
        ).also { person ->
            person.bostedsadresseperiode = if (perioder == null) null else perioder.map {
                GrBostedsadresseperiode(periode = it)
            }
        }
    }

    @Test
    fun `skal returnere usant dersom arbeidsforhold er null eller listen av arbeidsforhold er tom`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(null)))).isFalse()
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(emptyList())))).isFalse()
    }

    @Test
    fun `90 dagers arbeidsfravær i starten av 5-års-perioden skal godkjennes`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(90), tom = null)
                )
        )))).isTrue()
    }

    @Test
    fun `91 dagers arbeidsfravær i starten av 5-års-perioden skal avslås`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5).plusDays(91), tom = null)
                )
        )))).isFalse()
    }

    @Test
    fun `90 dagers arbeidsfravær i slutten av 5-års-perioden skal godkjennes`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(90))
                )
        )))).isTrue()
    }

    @Test
    fun `91 dagers arbeidsfravær i slutten av 5-års-perioden skal avslås`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(91))
                )
        )))).isFalse()
    }

    @Test
    fun `et mellomrom på 90 dager (eller mindre) mellom to arbeidsperioder skal godkjennes`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusDays(2), tom = LocalDate.now()),
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(93))
                )
        )))).isTrue()
    }

    @Test
    fun `et mellomrom på 91 dager (eller mer) mellom to arbeidsperioder skal avslås`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusDays(2), tom = LocalDate.now()),
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusDays(94))
                )
        )))).isFalse()
    }

    @Test
    fun `arbeidsforhold som startet fra før 5 år siden skal tas med i beregningen`() {
        assertThat(morHarJobbetINorgeSiste5År(Fakta(lagSøkerMedArbeidsforhold(
                listOf(
                        DatoIntervallEntitet(fom = LocalDate.now().minusYears(8))
                )
        )))).isTrue()
    }
}