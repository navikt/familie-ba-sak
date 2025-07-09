import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBigDecimal
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import java.math.BigDecimal
import java.time.LocalDate

fun lagSvarFraEcbMock(dataTable: DataTable): Map<Pair<String, LocalDate>, BigDecimal> =
    dataTable
        .asMaps()
        .map { rad ->
            val valutakursdato =
                parseValgfriDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTAKURSDATO, rad)
                    ?: throw Feil("Valutakursdato må være satt")
            val valutakode =
                parseValgfriString(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTA_KODE, rad)
                    ?: throw Feil("Valutakode må være satt")
            val kurs = parseBigDecimal(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.KURS, rad)

            Pair(valutakode, valutakursdato) to kurs
        }.toMap()
