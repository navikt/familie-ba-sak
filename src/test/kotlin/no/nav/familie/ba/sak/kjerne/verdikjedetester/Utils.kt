package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate


fun generellAssertFagsak(restFagsak: Ressurs<RestFagsak>,
                         fagsakStatus: FagsakStatus,
                         behandlingStegType: StegType? = null,
                         behandlingResultat: BehandlingResultat? = null) {
    if (restFagsak.status != Ressurs.Status.SUKSESS) throw IllegalStateException("generellAssertFagsak feilet. status: ${restFagsak.status.name},  melding: ${restFagsak.melding}")
    assertEquals(fagsakStatus, restFagsak.data?.status)
    if (behandlingStegType != null) {
        assertEquals(behandlingStegType, hentAktivBehandling(restFagsak = restFagsak.data!!)?.steg)
    }
    if (behandlingResultat != null) {
        assertEquals(behandlingResultat, hentAktivBehandling(restFagsak = restFagsak.data!!)?.resultat)
    }
}

fun assertUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode, antallBarn: Int, utbetaltPerMnd: Int) {
    assertEquals(antallBarn, utbetalingsperiode.utbetalingsperiodeDetaljer.size)
    assertEquals(utbetaltPerMnd, utbetalingsperiode.utbetaltPerMnd)
}

fun hentNåværendeEllerNesteMånedsUtbetaling(behandling: RestUtvidetBehandling): Int {
    val utbetalingsperioder =
            behandling.utbetalingsperioder.sortedBy { it.periodeFom }
    val nåværendeUtbetalingsperiode = utbetalingsperioder
            .firstOrNull { it.periodeFom.isBefore(LocalDate.now()) && it.periodeTom.isAfter(LocalDate.now()) }

    val nesteUtbetalingsperiode = utbetalingsperioder.firstOrNull { it.periodeFom.isAfter(LocalDate.now()) }

    return nåværendeUtbetalingsperiode?.utbetaltPerMnd ?: nesteUtbetalingsperiode?.utbetaltPerMnd ?: 0
}

fun hentAktivBehandling(restFagsak: RestFagsak): RestUtvidetBehandling? {
    return restFagsak.behandlinger.firstOrNull { it.aktiv }
}

fun hentAktivtVedtak(restFagsak: RestFagsak): RestVedtak? {
    return hentAktivBehandling(restFagsak)?.vedtakForBehandling?.firstOrNull { it.aktiv }
}
