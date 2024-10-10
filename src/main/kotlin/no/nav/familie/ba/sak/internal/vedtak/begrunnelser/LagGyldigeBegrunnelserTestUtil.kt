package no.nav.familie.ba.sak.internal.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun lagTeksterForGyldigeBegrunnelser(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String =
    hentTekstForGyligeBegrunnelserForVedtaksperiodene(vedtaksperioder) +
        hentTekstValgteBegrunnelser(behandlingId, vedtaksperioder) +
        hentTekstBrevPerioder(behandlingId, vedtaksperioder) +
        hentBrevBegrunnelseTekster(behandlingId, vedtaksperioder) +
        hentEØSBrevBegrunnelseTekster(behandlingId, vedtaksperioder)

private fun hentTekstForGyligeBegrunnelserForVedtaksperiodene(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Så forvent at følgende begrunnelser er gyldige
    | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |""" +
        hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder)

private fun hentVedtaksperiodeRaderForGyldigeBegrunnelser(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
    | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | |""" +
            if (vedtaksperiode.eøsBegrunnelser.isNotEmpty()) {
                """
    | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.type} | EØS_FORORDNINGEN | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |
    """
            } else {
                ""
            }
    }

private fun hentTekstValgteBegrunnelser(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Og når disse begrunnelsene er valgt for behandling $behandlingId
    | Fra dato | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |""" +
        hentValgteBegrunnelserRader(vedtaksperioder)

private fun hentValgteBegrunnelserRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
    | ${vedtaksperiode.fom?.tilddMMyyyy() ?: ""} |${vedtaksperiode.tom?.tilddMMyyyy() ?: ""} | ${vedtaksperiode.begrunnelser.joinToString { it.standardbegrunnelse.name }} | ${vedtaksperiode.eøsBegrunnelser.joinToString { it.begrunnelse.name }} | |"""
    }

private fun hentTekstBrevPerioder(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) =
    """

    Så forvent følgende brevperioder for behandling $behandlingId
    | Brevperiodetype  | Fra dato   | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |""" +
        hentBrevPeriodeRader(vedtaksperioder)

private fun hentBrevPeriodeRader(vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>) =
    vedtaksperioder.joinToString("") { vedtaksperiode ->
        """
    | | ${vedtaksperiode.fom?.tilMånedÅr() ?: ""} | ${vedtaksperiode.tom?.tilMånedÅr() ?: ""} | | | | |"""
    }

private fun hentBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String =
    vedtaksperioder.filter { (it.begrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
    | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |""" +
            vedtaksperiode.begrunnelser.map { it.standardbegrunnelse }.joinToString("") {
                """
    | $it | STANDARD |               |                      |             |                                      |         |       |                  |                         |                               |"""
            }
    }

private fun hentEØSBrevBegrunnelseTekster(
    behandlingId: Long?,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
): String =
    vedtaksperioder.filter { (it.eøsBegrunnelser).isNotEmpty() }.joinToString("") { vedtaksperiode ->
        """

    Så forvent følgende brevbegrunnelser for behandling $behandlingId i periode ${vedtaksperiode.fom?.tilddMMyyyy() ?: "-"} til ${vedtaksperiode.tom?.tilddMMyyyy() ?: "-"}
    | Begrunnelse | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | """ +
            vedtaksperiode.eøsBegrunnelser.map { it.begrunnelse }.joinToString("") {
                """
    | $it | EØS | | | | | | | | |"""
            }
    }
