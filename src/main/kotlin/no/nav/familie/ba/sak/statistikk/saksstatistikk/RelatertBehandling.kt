package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import java.time.LocalDateTime

data class RelatertBehandling(
    val id: String,
    val vedtattTidspunkt: LocalDateTime,
    val fagsystem: Fagsystem,
) {
    enum class Fagsystem {
        BA,
        KLAGE,
    }

    companion object Factory {
        fun fraBarnetrygdbehandling(barnetrygdbehandling: Behandling) =
            RelatertBehandling(
                id = barnetrygdbehandling.id.toString(),
                vedtattTidspunkt = barnetrygdbehandling.aktivertTidspunkt,
                fagsystem = Fagsystem.BA,
            )

        fun fraKlagebehandling(klagebehandling: KlagebehandlingDto): RelatertBehandling {
            val vedtaksdato = klagebehandling.vedtaksdato
            if (vedtaksdato == null) {
                throw Feil("Forventer vedtaksdato for klagebehandling ${klagebehandling.id}")
            }
            return RelatertBehandling(
                id = klagebehandling.id.toString(),
                vedtattTidspunkt = vedtaksdato,
                fagsystem = Fagsystem.KLAGE,
            )
        }
    }
}
