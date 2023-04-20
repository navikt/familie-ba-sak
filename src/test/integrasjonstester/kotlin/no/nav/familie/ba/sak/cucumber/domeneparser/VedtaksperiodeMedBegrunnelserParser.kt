package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

object VedtaksperiodeMedBegrunnelserParser {

    fun mapForventetVedtaksperioderMedBegrunnelser(
        dataTable: DataTable,
        vedtak: Vedtak
    ): List<VedtaksperiodeMedBegrunnelser> {
        return dataTable.asMaps().map { rad ->
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = parseDato(Domenebegrep.FRA_DATO, rad),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                type = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.VEDTAKSPERIODE_TYPE, rad)
            )
        }
    }

    enum class DomenebegrepPersongrunnlag(override val nøkkel: String) : Domenenøkkel {
        PERSON_TYPE("Persontype"),
        FØDSELSDATO("Fødselsdato"),
        PERSON_ID("PersonId")
    }

    enum class DomenebegrepVedtaksperiodeMedBegrunnelser(override val nøkkel: String) : Domenenøkkel {
        VEDTAKSPERIODE_TYPE("Vedtaksperiodetype"),
        VILKÅR("Vilkår"),
        RESULTAT("Resultat")
    }
}
