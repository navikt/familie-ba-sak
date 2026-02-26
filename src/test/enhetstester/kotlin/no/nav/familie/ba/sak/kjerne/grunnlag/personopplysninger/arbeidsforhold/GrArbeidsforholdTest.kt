package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrArbeidsforholdTest {
    private val arbeidsforhold =
        GrArbeidsforhold(
            id = 0,
            person = tilfeldigPerson(),
            periode = DatoIntervallEntitet(fom = LocalDate.of(2000, 1, 1), tom = LocalDate.of(2000, 12, 31)),
            arbeidsgiverId = "123123123",
            arbeidsgiverType = "Organisasjon",
        )

    @Nested
    inner class Equals {
        @Test
        fun `skal returnere false hvis person er ulik`() {
            val annetArbeidsforhold = arbeidsforhold.copy(person = tilfeldigPerson())
            assert(arbeidsforhold != annetArbeidsforhold)
        }

        @Test
        fun `skal returnere false hvis periode er ulik`() {
            val annetArbeidsforhold = arbeidsforhold.copy(periode = DatoIntervallEntitet(fom = LocalDate.of(2001, 1, 1), tom = LocalDate.of(2001, 12, 31)))
            assert(arbeidsforhold != annetArbeidsforhold)
        }

        @Test
        fun `skal returnere false hvis arbeidsgiverId er ulik`() {
            val annetArbeidsforhold = arbeidsforhold.copy(arbeidsgiverId = "321321321")
            assert(arbeidsforhold != annetArbeidsforhold)
        }

        @Test
        fun `skal returnere false hvis arbeidsgiverType er ulik`() {
            val annetArbeidsforhold = arbeidsforhold.copy(arbeidsgiverType = "Person")
            assert(arbeidsforhold != annetArbeidsforhold)
        }

        @Test
        fun `skal returnere true hvis alle felter er like`() {
            val annetArbeidsforhold = arbeidsforhold.copy(id = 1)
            assert(arbeidsforhold == annetArbeidsforhold)
        }
    }
}
