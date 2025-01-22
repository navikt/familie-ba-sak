package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utbetalingsland
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KompetanseTest {
    @Test
    fun `tilUtbetalingsland - skal returnere landkode for utbetalingsland dersom kompetanse er utfylt`() {
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitetsland = "SE",
                barnetsBostedsland = "SE",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        val utbetalingsland = kompetanse.utbetalingsland()

        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("SE")
    }

    @Test
    fun `tilUtbetalingsland - skal returnere null for utbetalingsland dersom kompetanse ikke er utfylt`() {
        val kompetanse =
            lagKompetanse()

        val utbetalingsland = kompetanse.utbetalingsland()

        assertThat(utbetalingsland).isNull()
    }

    @Test
    fun `tilUtbetalingsland - skal returnere Norge for utbetalingsland dersom kompetanse er utfylt og resultatet er at norge er primærland`() {
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                annenForeldersAktivitetsland = "SE",
                barnetsBostedsland = "SE",
                kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        val utbetalingsland = kompetanse.utbetalingsland()

        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("NO")
    }

    @Test
    fun `tilUtbetalingsland - skal returnere utbetalingsland ulikt Norge dersom kompetanse er utfylt og hovedregelen fortsatt gir Norge`() {
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
                søkersAktivitetsland = "SE",
                annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
                annenForeldersAktivitetsland = null,
                barnetsBostedsland = "NO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        val utbetalingsland = kompetanse.utbetalingsland()

        assertThat(utbetalingsland).isNotNull
        assertThat(utbetalingsland).isEqualTo("SE")
    }

    @Test
    fun `tilUtbetalingsland - skal kaste feil dersom kompetanse er utfylt og det ikke er registrert noe annet land enn Norge på kompetansen`() {
        val kompetanse =
            lagKompetanse(
                barnAktører = setOf(randomAktør()),
                søkersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
                annenForeldersAktivitetsland = null,
                barnetsBostedsland = "NO",
                kompetanseResultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                erAnnenForelderOmfattetAvNorskLovgivning = false,
            )

        val funksjonellFeil = assertThrows<FunksjonellFeil> { kompetanse.utbetalingsland() }

        assertThat(funksjonellFeil.melding).isEqualTo("Dersom Norge er sekundærland, må søkers aktivitetsland, annen forelders aktivitetsland eller barnets bostedsland være satt til noe annet enn Norge.")
    }
}
