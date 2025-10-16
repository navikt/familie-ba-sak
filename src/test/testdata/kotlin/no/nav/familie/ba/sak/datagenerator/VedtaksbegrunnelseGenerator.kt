package no.nav.familie.ba.sak.datagenerator

import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate

fun lagVedtaksbegrunnelse(
    standardbegrunnelse: Standardbegrunnelse =
        Standardbegrunnelse.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk(),
) = Vedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    standardbegrunnelse = standardbegrunnelse,
)

fun lagEØSBegrunnelse(
    id: Long = 0L,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
    begrunnelse: EØSStandardbegrunnelse,
): EØSBegrunnelse =
    EØSBegrunnelse(
        id = id,
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        begrunnelse = begrunnelse,
    )

fun lagBegrunnelseData(
    standardbegrunnelse: Standardbegrunnelse,
): BegrunnelseData =
    BegrunnelseData(
        vedtakBegrunnelseType = standardbegrunnelse.vedtakBegrunnelseType,
        apiNavn = standardbegrunnelse.sanityApiNavn,
        gjelderSoker = true,
        barnasFodselsdatoer = LocalDate.now().minusYears(2).toString(),
        antallBarn = 1,
        maanedOgAarBegrunnelsenGjelderFor = LocalDate.now().toString(),
        maalform = "NB",
        belop = "1 234",
        soknadstidspunkt = LocalDate.now().toString(),
        avtaletidspunktDeltBosted = LocalDate.now().toString(),
        sokersRettTilUtvidet = "ja",
    )
