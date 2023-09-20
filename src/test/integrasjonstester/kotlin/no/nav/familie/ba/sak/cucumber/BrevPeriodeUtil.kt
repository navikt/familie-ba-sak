package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevPeriodeParser
import no.nav.familie.ba.sak.cucumber.domeneparser.norskDatoFormatter
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.SøkersRettTilUtvidet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import java.time.LocalDate

fun parseStandardBegrunnelser(dataTable: DataTable) = dataTable.asMaps().map { rad ->
    BegrunnelseData(
        vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
        apiNavn = parseEnum<Standardbegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        ).sanityApiNavn,

        gjelderSoker = parseBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad),
        barnasFodselsdatoer = parseString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDAGER,
            rad,
        ),

        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = "",
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = "",

        antallBarn = parseInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad),

        antallBarnOppfyllerTriggereOgHarUtbetaling = 0,
        antallBarnOppfyllerTriggereOgHarNullutbetaling = 0,

        maanedOgAarBegrunnelsenGjelderFor = parseString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR,
            rad,
        ),
        maalform = parseEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad).tilSanityFormat(),
        belop = parseString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BELØP, rad),
        soknadstidspunkt = parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKNADSTIDSPUNKT,
            rad,
        ) ?: "",
        avtaletidspunktDeltBosted = parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.AVTALETIDSPUNKT_DELT_BOSTED,
            rad,
        ) ?: "",
        sokersRettTilUtvidet = parseValgfriEnum<SøkersRettTilUtvidet>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKERS_RETT_TIL_UTVIDET,
            rad,
        )?.tilSanityFormat() ?: SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT.tilSanityFormat(),
    )
}

fun parseNullableDato(fom: String) = if (fom.uppercase() in listOf("NULL", "-", "")) {
    null
} else {
    LocalDate.parse(
        fom,
        norskDatoFormatter,
    )
}
