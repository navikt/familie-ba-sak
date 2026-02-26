package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrOppholdTest {
    private val opphold =
        GrOpphold(
            id = 0,
            person = tilfeldigPerson(),
            gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.of(2000, 1, 1), tom = LocalDate.of(2000, 12, 31)),
            type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
        )

    @Nested
    inner class Equals {
        @Test
        fun `skal returnere false hvis person er ulik`() {
            val annetOpphold = opphold.copy(person = tilfeldigPerson())
            assert(opphold != annetOpphold)
        }

        @Test
        fun `skal returnere false hvis periode er ulik`() {
            val annetOpphold = opphold.copy(gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.of(2001, 1, 1), tom = LocalDate.of(2001, 12, 31)))
            assert(opphold != annetOpphold)
        }

        @Test
        fun `skal returnere false hvis type er ulik`() {
            val annetOpphold = opphold.copy(type = OPPHOLDSTILLATELSE.PERMANENT)
            assert(opphold != annetOpphold)
        }

        @Test
        fun `skal returnere true hvis alle felter er like`() {
            val annetOpphold = opphold.copy(id = 1)
            assert(opphold == annetOpphold)
        }
    }
}
