package no.nav.familie.ba.sak.kjerne.dokument

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.UregistrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.v2byggBegrunnelserOgFritekster
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetaljEnkel
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestReporter
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

data class BrevBegrunnelseTestConfig(
    val beskrivelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljEnkel>,
    val standardbegrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String>,
    val begrunnelsepersoner: List<BegrunnelsePerson>,
    val målform: Målform,
    val uregistrerteBarn: List<UregistrertBarnEnkel>,
    val forventetOutput: List<BegrunnelseData>
)

class BrevbegrunnelseTest {

    @Test
    fun test(testReporter: TestReporter) {
        val testmappe = File("./src/test/resources/brevbegrunnelseCaser")

        val antallFeil = testmappe.list()?.fold(0) { acc, it ->
            val fil = File("./src/test/resources/brevbegrunnelseCaser/$it")
            val behandlingsresultatPersonTestConfig =
                objectMapper.readValue<BrevBegrunnelseTestConfig>(fil.readText())

            val begrunnelser: List<BegrunnelseData> = v2byggBegrunnelserOgFritekster(
                fom = behandlingsresultatPersonTestConfig.fom,
                tom = behandlingsresultatPersonTestConfig.tom,
                utbetalingsperiodeDetaljerEnkel = behandlingsresultatPersonTestConfig.utbetalingsperiodeDetaljer,
                standardbegrunnelser = behandlingsresultatPersonTestConfig.standardbegrunnelser,
                fritekster = behandlingsresultatPersonTestConfig.fritekster,
                begrunnelsepersonerIBehandling = behandlingsresultatPersonTestConfig.begrunnelsepersoner,
                målform = behandlingsresultatPersonTestConfig.målform,
                uregistrerteBarn = behandlingsresultatPersonTestConfig.uregistrerteBarn
            ).map { it as BegrunnelseData }

            if (behandlingsresultatPersonTestConfig.forventetOutput.single().barnasFodselsdatoer != begrunnelser.single().barnasFodselsdatoer) {
                testReporter.publishEntry(
                    it,
                    "${behandlingsresultatPersonTestConfig.beskrivelse}\nForventet ${behandlingsresultatPersonTestConfig.forventetOutput.single().barnasFodselsdatoer}, men fikk ${begrunnelser.single().barnasFodselsdatoer}."
                )
                acc + 1
            } else {
                0
            }
        }

        assert(antallFeil == 0)
    }

    @Test
    fun `Skal lage begrunnelser for innvilgelsesperiode`() {
        val barn = tilfeldigPerson(personType = PersonType.BARN)

        val fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned()
        val tom = LocalDate.now().plusMonths(2).sisteDagIMåned()
        val begrunnelser = v2byggBegrunnelserOgFritekster(
            fom = fom,
            tom = tom,
            utbetalingsperiodeDetaljerEnkel = listOf(
                UtbetalingsperiodeDetaljEnkel(
                    personIdent = barn.aktør.aktivIdent().fødselsnummer,
                    utbetaltPerMnd = 1054,
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                )
            ),
            standardbegrunnelser = listOf(
                RestVedtaksbegrunnelse(
                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER,
                    vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
                    personIdenter = listOf(barn.aktør.aktivIdent().fødselsnummer)
                )
            ),
            fritekster = emptyList(),
            begrunnelsepersonerIBehandling = listOf(barn.tilBegrunnelsePerson()),
            målform = Målform.NB,
            uregistrerteBarn = emptyList()
        )

        assertEquals(1, begrunnelser.size)
    }
}
