package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.mockk
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.brev.lagMinimertPerson
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BrevPeriodeUtilTest {

    private val featureToggleService: FeatureToggleService = mockk()

    @Test
    fun `Skal sortere perioder kronologisk, med avslag til slutt`() {
        val liste = listOf(
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(12),
                tom = LocalDate.now().minusMonths(8),
                type = Vedtaksperiodetype.UTBETALING,
                featureToggleService = featureToggleService,
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(4),
                tom = null,
                type = Vedtaksperiodetype.AVSLAG,
                featureToggleService = featureToggleService,
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(7),
                tom = LocalDate.now().minusMonths(4),
                type = Vedtaksperiodetype.OPPHØR,
                featureToggleService = featureToggleService,
            ),
            lagBrevperiodeData(
                fom = LocalDate.now().minusMonths(3),
                tom = LocalDate.now(),
                type = Vedtaksperiodetype.UTBETALING,
                featureToggleService = featureToggleService,
            ),
        )

        val sortertListe = liste.sorted()

        Assertions.assertTrue(sortertListe.size == 4)
        val førstePeriode = sortertListe.first()
        val andrePeriode = sortertListe[1]
        val tredjePeriode = sortertListe[2]
        val sistePeriode = sortertListe.last()

        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, førstePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(12), førstePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.OPPHØR, andrePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(7), andrePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.UTBETALING, tredjePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(3), tredjePeriode.minimertVedtaksperiode.fom)
        Assertions.assertEquals(Vedtaksperiodetype.AVSLAG, sistePeriode.minimertVedtaksperiode.type)
        Assertions.assertEquals(LocalDate.now().minusMonths(4), sistePeriode.minimertVedtaksperiode.fom)
    }

    @Test
    fun `Skal plukke ut kompetansene i perioden`() {
        val barnAktør1 = Aktør(aktørId = "1111111111111")
        val barnAktør2 = Aktør(aktørId = "2222222222222")

        val periode1 = MånedPeriode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        val periode2 = MånedPeriode(YearMonth.of(2021, 2), YearMonth.of(2021, 3))
        val periode3 = MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 5))

        val kompetanse1 =
            lagKompetanse(fom = periode1.fom, tom = periode1.tom, barnAktører = setOf(barnAktør1))
        val kompetanse2 =
            lagKompetanse(
                fom = periode2.fom,
                tom = periode2.tom,
                barnAktører = setOf(barnAktør1),
                søkersAktivitet = SøkersAktivitet.ARBEIDER,
            )
        val kompetanse3 =
            lagKompetanse(
                fom = periode2.fom,
                tom = periode3.tom,
                barnAktører = setOf(barnAktør2),
                søkersAktivitet = SøkersAktivitet.INAKTIV,
            )
        val kompetanse4 =
            lagKompetanse(fom = periode3.fom, tom = periode3.tom, barnAktører = setOf(barnAktør1))

        Assertions.assertEquals(
            listOf(kompetanse1, kompetanse2, kompetanse3.copy(tom = periode2.tom)),
            listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)
                .hentIPeriode(periode1.fom, periode2.tom),
        )
    }

    @Test
    fun `Skal kunne kombinere registrerte og uregistrerte barns fødselsdatoer til avslagsbegrunnelse`() {
        val barnIBegrunnelse = listOf(
            lagMinimertPerson(fødselsdato = LocalDate.of(2021, 1, 1), type = PersonType.BARN),
            lagMinimertPerson(fødselsdato = LocalDate.of(2021, 2, 2), type = PersonType.BARN),
        ).map { it.tilMinimertRestPerson() }
        val barnPåBehandling = listOf(
            lagMinimertPerson(fødselsdato = LocalDate.of(2021, 1, 1), type = PersonType.BARN),
            lagMinimertPerson(fødselsdato = LocalDate.of(2021, 2, 2), type = PersonType.BARN),
            lagMinimertPerson(fødselsdato = LocalDate.of(2021, 3, 3), type = PersonType.BARN),
        ).map { it.tilMinimertRestPerson() }
        val uregistrerteBarn = listOf(
            MinimertUregistrertBarn(personIdent = "", navn = "Ole", fødselsdato = LocalDate.of(2021, 4, 4)),
            MinimertUregistrertBarn(personIdent = "", navn = "Dole", fødselsdato = LocalDate.of(2021, 5, 5)),
            MinimertUregistrertBarn(personIdent = "", navn = "Doffen", fødselsdato = LocalDate.of(2021, 6, 6)),
        )

        Assertions.assertEquals(
            hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                barnIBegrunnelse,
                barnPåBehandling,
                uregistrerteBarn,
                gjelderSøker = true,
            ),
            "01.01.21, 02.02.21, 03.03.21, 04.04.21, 05.05.21 og 06.06.21",
        )
        Assertions.assertEquals(
            hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                barnIBegrunnelse,
                barnPåBehandling,
                uregistrerteBarn,
                gjelderSøker = false,
            ),
            "01.01.21, 02.02.21, 04.04.21, 05.05.21 og 06.06.21",
        )
        Assertions.assertEquals(
            hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                barnIBegrunnelse,
                barnPåBehandling,
                emptyList(),
                gjelderSøker = true,
            ),
            "01.01.21, 02.02.21 og 03.03.21",
        )
        Assertions.assertEquals(
            hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                emptyList(),
                emptyList(),
                uregistrerteBarn,
                gjelderSøker = true,
            ),
            "04.04.21, 05.05.21 og 06.06.21",
        )
    }
}

private fun lagBrevperiodeData(fom: LocalDate?, tom: LocalDate?, type: Vedtaksperiodetype, featureToggleService: FeatureToggleService): BrevperiodeData {
    val restBehandlingsgrunnlagForBrev = RestBehandlingsgrunnlagForBrev(
        personerPåBehandling = emptyList(),
        minimertePersonResultater = emptyList(),
        minimerteEndredeUtbetalingAndeler = emptyList(),
        fagsakType = FagsakType.NORMAL,
    )
    return BrevperiodeData(
        restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
        erFørsteVedtaksperiodePåFagsak = false,
        brevMålform = Målform.NB,
        minimertVedtaksperiode = MinimertVedtaksperiode(
            begrunnelser = emptyList(),
            fom = fom,
            tom = tom,
            type = type,
            eøsBegrunnelser = emptyList(),
        ),
        uregistrerteBarn = emptyList(),
        minimerteKompetanserForPeriode = emptyList(),
        minimerteKompetanserSomStopperRettFørPeriode = emptyList(),
        dødeBarnForrigePeriode = emptyList(),
        featureToggleService = featureToggleService,
    )
}
