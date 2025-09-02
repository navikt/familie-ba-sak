package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevPeriodeParser
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse

fun parseBrevPerioder(dataTable: DataTable): List<BrevPeriode> =
    dataTable.asMaps().map { rad: Tabellrad ->

        val beløp = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevPeriode.BELØP, rad)?.replace(' ', ' ') ?: ""
        val antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevPeriode.ANTALL_BARN, rad) ?: -1
        val barnasFodselsdager =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevPeriode.BARNAS_FØDSELSDAGER,
                rad,
            ) ?: ""
        val duEllerInstitusjonen =
            parseValgfriString(BrevPeriodeParser.DomenebegrepBrevPeriode.DU_ELLER_INSTITUSJONEN, rad) ?: "Du"

        BrevPeriode(
            fom = parseValgfriString(Domenebegrep.FRA_DATO, rad) ?: "",
            tom = parseValgfriString(Domenebegrep.TIL_DATO, rad)?.removeSurrounding("\"") ?: "",
            beløp = beløp,
            // egen test for dette. Se `forvent følgende brevbegrunnelser for behandling i periode`()
            begrunnelser = emptyList<BrevBegrunnelse>(),
            brevPeriodeType = parseEnum(BrevPeriodeParser.DomenebegrepBrevPeriode.TYPE, rad),
            antallBarn = antallBarn.toString(),
            barnasFodselsdager = barnasFodselsdager,
            duEllerInstitusjonen = duEllerInstitusjonen,
        )
    }
