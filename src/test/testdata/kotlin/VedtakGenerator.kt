import no.nav.familie.ba.sak.common.nesteVedtakId
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import java.time.LocalDateTime

fun lagVedtak(
    behandling: Behandling = lagBehandling(),
    stønadBrevPdF: ByteArray? = null,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
) = Vedtak(
    id = nesteVedtakId(),
    behandling = behandling,
    vedtaksdato = vedtaksdato,
    stønadBrevPdF = stønadBrevPdF,
)
