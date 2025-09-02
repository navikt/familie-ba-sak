package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevPeriodeParser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.norskDatoFormatter
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.SøkersRettTilUtvidet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import java.time.LocalDate

typealias Tabellrad = Map<String, String>

enum class Begrunnelsetype {
    EØS,
    STANDARD,
}

fun parseBegrunnelser(dataTable: DataTable): List<BegrunnelseMedData> =
    dataTable.asMaps().map { rad: Tabellrad ->

        val type =
            parseValgfriEnum<Begrunnelsetype>(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.TYPE,
                rad,
            ) ?: Begrunnelsetype.STANDARD

        when (type) {
            Begrunnelsetype.STANDARD -> parseStandardBegrunnelse(rad)
            Begrunnelsetype.EØS -> parseEøsBegrunnelse(rad)
        }
    }

fun parseStandardBegrunnelse(rad: Tabellrad): BegrunnelseData {
    val begrunnelse =
        parseEnum<Standardbegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        )

    return BegrunnelseData(
        vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
        apiNavn =
            begrunnelse.sanityApiNavn,
        gjelderSoker = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad) ?: false,
        barnasFodselsdatoer =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
                rad,
            ) ?: "",
        antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad) ?: 0,
        maanedOgAarBegrunnelsenGjelderFor =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR,
                rad,
            ),
        maalform =
            (parseValgfriEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad) ?: Målform.NB)
                .tilSanityFormat(),
        belop = parseValgfriString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BELØP, rad)?.replace(' ', ' ') ?: "",
        soknadstidspunkt =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKNADSTIDSPUNKT,
                rad,
            ) ?: "",
        avtaletidspunktDeltBosted =
            parseValgfriString(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.AVTALETIDSPUNKT_DELT_BOSTED,
                rad,
            ) ?: "",
        sokersRettTilUtvidet =
            parseValgfriEnum<SøkersRettTilUtvidet>(
                BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKERS_RETT_TIL_UTVIDET,
                rad,
            )?.tilSanityFormat() ?: SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT.tilSanityFormat(),
    )
}

fun parseEøsBegrunnelse(rad: Tabellrad): EØSBegrunnelseData {
    val gjelderSoker = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad) ?: false

    val annenForeldersAktivitet =
        parseValgfriEnum<KompetanseAktivitet>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
            rad,
        )
    val annenForeldersAktivitetsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
            rad,
        )
    val barnetsBostedsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
            rad,
        )
    val søkersAktivitet =
        parseValgfriEnum<KompetanseAktivitet>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
            rad,
        )
    val søkersAktivitetsland =
        parseValgfriString(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
            rad,
        )

    val erAnnenForelderOmfattetAvNorskLovgivning =
        parseValgfriBoolean(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ER_ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING,
            rad,
        ) ?: false

    val begrunnelse =
        parseEnum<EØSStandardbegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        )

    val barnasFodselsdatoer =
        parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
            rad,
        ) ?: ""

    val antallBarn = parseValgfriInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad) ?: -1

    val målform = (parseValgfriEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad) ?: Målform.NB).tilSanityFormat()

    return if (annenForeldersAktivitet != null) {
        if (annenForeldersAktivitetsland == null ||
            barnetsBostedsland == null ||
            søkersAktivitet == null ||
            søkersAktivitetsland == null
        ) {
            throw Feil("Alle felter for kompetanse må fylles ut dersom ett av dem fylles ut.")
        }

        EØSBegrunnelseDataMedKompetanse(
            vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
            apiNavn = begrunnelse.sanityApiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,
            antallBarn = antallBarn,
            maalform = målform,
            annenForeldersAktivitet = annenForeldersAktivitet,
            annenForeldersAktivitetsland = annenForeldersAktivitetsland,
            barnetsBostedsland = barnetsBostedsland,
            sokersAktivitet = søkersAktivitet,
            sokersAktivitetsland = søkersAktivitetsland,
            gjelderSoker = gjelderSoker,
            erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
        )
    } else {
        EØSBegrunnelseDataUtenKompetanse(
            vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
            apiNavn = begrunnelse.sanityApiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,
            antallBarn = antallBarn,
            maalform = målform,
            gjelderSoker = gjelderSoker ?: false,
        )
    }
}

fun parseNullableDato(fom: String) =
    if (fom.uppercase() in listOf("NULL", "-", "")) {
        null
    } else {
        LocalDate.parse(
            fom,
            norskDatoFormatter,
        )
    }
