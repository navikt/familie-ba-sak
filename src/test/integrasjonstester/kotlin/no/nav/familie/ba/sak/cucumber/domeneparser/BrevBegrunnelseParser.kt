package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.SammenlignbarBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse

object BrevBegrunnelseParser {

    fun mapStandardBegrunnelser(dataTable: DataTable): List<SammenlignbarBegrunnelse> {
        return dataTable.asMaps().map { rad ->
            SammenlignbarBegrunnelse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                type = parseEnum(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.VEDTAKSPERIODE_TYPE, rad),
                inkluderteStandardBegrunnelser = parseEnumListe<Standardbegrunnelse>(
                    DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.INKLUDERTE_BEGRUNNELSER,
                    rad,
                ).toSet(),
                ekskluderteStandardBegrunnelser = parseEnumListe<Standardbegrunnelse>(
                    DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.EKSKLUDERTE_BEGRUNNELSER,
                    rad,
                ).toSet(),
            )
        }
    }

    enum class DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser(override val nøkkel: String) : Domenenøkkel {
        VEDTAKSPERIODE_TYPE("VedtaksperiodeType"),
        INKLUDERTE_BEGRUNNELSER("Inkluderte Begrunnelser"),
        EKSKLUDERTE_BEGRUNNELSER("Ekskluderte Begrunnelser"),
    }
}
