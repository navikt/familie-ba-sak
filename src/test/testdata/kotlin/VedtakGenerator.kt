import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
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
