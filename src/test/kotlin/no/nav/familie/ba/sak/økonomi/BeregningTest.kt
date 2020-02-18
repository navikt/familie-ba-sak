package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.beregnUtbetalingsperioder
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.behandling.domene.vedtak.Ytelsetype.*
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningTest(
) {
    val fagsak = Fagsak(null, null, PersonIdent("12345"), FagsakStatus.OPPRETTET)
    val behandling = Behandling(fagsak = fagsak,
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                kategori = BehandlingKategori.NATIONAL,
                                underkategori = BehandlingUnderkategori.ORDINÆR)

    val vedtak = Vedtak(behandling = behandling,
                        ansvarligSaksbehandler = "ansvarligSaksbehandler",
                        vedtaksdato = LocalDate.now(),
                        stønadBrevMarkdown = "",
                        resultat = VedtakResultat.INNVILGET)

    /**
     * Testen generer 3 barn. 2 av dem er født dd. og 1 er født 2 år frem i tid.
     * Videre generer vi tidslinje for utbetaling med stønad fom og tom for samtlige barn.
     *
     * Barn 1: 18 år med barnetrygd fra sin fødselsdato
     * Barn 2: 18 år med barnetrygd fra sin fødselsdato
     * Barn 3: 15 år med barnetrygd fra 3 år etter sin fødselsdato
     *
     * Dette medfører følgende tidslinje:
     * 1 periode: 1 barn = 1054
     * 2 periode: 2 barn = 2108
     * 3 periode: 3 barn = 3162
     * 4 periode: 1 barn = 1054
     */
    @Test
    fun `Skal sjekke at tidslinjen for ordinær barnetrygd for 3 barn blir riktig`() {

        val tidslinjeMap = beregnUtbetalingsperioder(listOf(
                lagPersonVedtak(ORDINÆR_BARNETRYGD, "2020-04-01", "2038-03-31", 1054),
                lagPersonVedtak(ORDINÆR_BARNETRYGD, "2022-04-01", "2040-03-31", 1054),
                lagPersonVedtak(ORDINÆR_BARNETRYGD, "2023-04-01", "2038-03-31", 1054)))

        val forventedeSegmenter = listOf(
                lagSegmentBeløp("2020-04-01","2022-03-31", 1054),
                lagSegmentBeløp("2022-04-01","2023-03-31", 2108),
                lagSegmentBeløp("2023-04-01","2038-03-31", 3162),
                lagSegmentBeløp("2038-04-01","2040-03-31", 1054)
        )

        assertLikeSegmenter(forventedeSegmenter, tidslinjeMap["BATR"])
    }

    @Test
    fun `Skal sjekke at tidslinjen for ordinær barnetrygd og småbarnstillegg blir riktig`() {

        // Barn født 27/9/2017 -> Barnetrygd 1/10/2017-30/9/2035, småbarnstillegg 1/10/2017-30/9/2020
        // Barn født 18/4/2020 -> Barnetrygd 1/5/2020-30/4/2038, småbarnstillegg 1/5/2020-30/4/2023
        // Utvidet barnetrygd 1/4/2020 - 31/1/2021

        val tidslinjeMap = beregnUtbetalingsperioder(listOf(
                lagPersonVedtak(SMÅBARNSTILLEGG, "2020-04-01", "2023-03-31", 660),
                lagPersonVedtak(ORDINÆR_BARNETRYGD, "2020-04-01", "2038-03-31", 1054),
                lagPersonVedtak(UTVIDET_BARNETRYGD, "2020-04-01", "2021-01-31", 1054)))

        val forventedeSegmenterBarnetrygd = listOf(
                lagSegmentBeløp("2020-04-01","2021-01-31", 2108),
                lagSegmentBeløp("2021-02-01","2038-03-31", 1054)
        )

        val forventedeSegmenterSmåbarnstillegg = listOf(
                lagSegmentBeløp("2020-04-01","2023-03-31", 660)
        )

        Assertions.assertEquals(2,tidslinjeMap.size)
        assertLikeSegmenter(forventedeSegmenterBarnetrygd, tidslinjeMap["BATR"])
        assertLikeSegmenter(forventedeSegmenterSmåbarnstillegg, tidslinjeMap["BATRSMA"])
    }


    private fun assertLikeSegmenter(forventedeSegmenter: List<LocalDateSegment<Int>>,
                                    tidslinje: LocalDateTimeline<Int>?) {
        Assertions.assertEquals(forventedeSegmenter.size, tidslinje!!.size(), "Forskjellig antall tidssegmenter")

        // Sjekk at periodene har riktig beløp
        tidslinje.toSegments().forEachIndexed { index, localDateSegment ->
            Assertions.assertEquals(forventedeSegmenter[index].value, localDateSegment.value, "Avvikende beløp")
            Assertions.assertEquals(forventedeSegmenter[index].fom, localDateSegment.fom, "Forskjell i fra og med")
            Assertions.assertEquals(forventedeSegmenter[index].tom, localDateSegment.tom, "Forskjell i til og med")
        }
    }

    fun tilfeldigPerson() = Person(
            personIdent = PersonIdent("00000000001"),
            fødselsdato = LocalDate.now(),
            type = PersonType.BARN,
            personopplysningGrunnlag = PersonopplysningGrunnlag(null)
    )

    fun lagPersonVedtak(ytelsetype: Ytelsetype, fom: String, tom: String, beløp: Int) : VedtakPerson {
       return VedtakPerson(
               person = tilfeldigPerson(),
               stønadFom = dato(fom),
               stønadTom = dato(tom),
               beløp = beløp,
               vedtak = vedtak,
               type = ytelsetype
        )

    }

    fun lagSegmentBeløp(fom: String, tom: String, beløp : Int) : LocalDateSegment<Int> =
            LocalDateSegment(dato(fom), dato(tom), beløp)

    fun dato(datoSomString : String) :LocalDate {
        return LocalDate.parse(datoSomString)
    }
}


