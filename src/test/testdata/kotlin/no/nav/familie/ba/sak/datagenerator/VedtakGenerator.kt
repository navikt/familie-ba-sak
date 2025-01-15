package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

fun lagVedtak(
    behandling: Behandling = lagBehandling(),
    stønadBrevPdF: ByteArray? = null,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
) = Vedtak(
    id = Random.nextLong(10000000),
    behandling = behandling,
    vedtaksdato = vedtaksdato,
    stønadBrevPdF = stønadBrevPdF,
)

fun lagVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak = lagVedtak(),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(lagVedtaksbegrunnelse()),
    eøsBegrunnelser: MutableSet<EØSBegrunnelse> = mutableSetOf(),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster,
    eøsBegrunnelser = eøsBegrunnelser,
)

fun lagUtvidetVedtaksperiodeMedBegrunnelser(
    id: Long = Random.nextLong(10000000),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: List<Vedtaksbegrunnelse> = listOf(lagVedtaksbegrunnelse()),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
    utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
    eøsBegrunnelser: List<EØSBegrunnelse> = emptyList(),
) = UtvidetVedtaksperiodeMedBegrunnelser(
    id = id,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster.map { it.fritekst },
    utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer,
    eøsBegrunnelser = eøsBegrunnelser,
)

fun leggTilBegrunnelsePåVedtaksperiodeIBehandling(
    behandling: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
        vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    if (behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = perisisterteVedtaksperioder.first { it.type == Vedtaksperiodetype.UTBETALING }.id,
            standardbegrunnelserFraFrontend =
                listOf(
                    Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                ),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )
    } else {
        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = perisisterteVedtaksperioder.first().id,
            standardbegrunnelserFraFrontend =
                listOf(
                    Standardbegrunnelse.FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET,
                ),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )
    }
}
