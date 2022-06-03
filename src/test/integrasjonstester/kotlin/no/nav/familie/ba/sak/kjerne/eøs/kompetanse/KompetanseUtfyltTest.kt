package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.felles.UtfyltStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.BarnetsBostedsland
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KompetanseUtfyltTest {

    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter i skjema er fylt ut`() {
        val kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.I_ARBEID,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            annenForeldersAktivitetsland = "NORGE",
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til OK dersom alle felter unntatt annenForeldersAktivitetsland er fylt ut og annenForeldersAktivitet er IKKE_AKTUELT eller INAKTIV`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.IKKE_AKTUELT,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG dersom alle felter unntatt annenForeldersAktivitetsland er fylt ut og annenForeldersAktivitet ikke er IKKE_AKTUELT eller INAKTIV`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.I_ARBEID,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.MOTTAR_PENSJON,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.FORSIKRET_I_BOSTEDSLAND,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG dersom 1 til 4 felter er satt med unntak av regel om annenForeldersAktivitet`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.IKKE_AKTUELT,
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.INAKTIV,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.FORSIKRET_I_BOSTEDSLAND,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = AnnenForeldersAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
            barnetsBostedsland = BarnetsBostedsland.Norge.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = SøkersAktivitet.ARBEIDER_I_NORGE
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT dersom ingen av feltene er utfylt`() {
        val kompetanse = lagKompetanse()

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.IKKE_UTFYLT, restKompetanse.status)
    }
}
