package no.nav.familie.ba.sak.dataGenerator.vedtak

import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun lagVedtaksbegrunnelse(
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon =
        VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    personIdenter: List<String> = listOf(tilfeldigPerson().aktør.aktivFødselsnummer()),
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk()
) = Vedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
    personIdenter = personIdenter,
)