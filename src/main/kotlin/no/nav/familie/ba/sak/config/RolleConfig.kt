package no.nav.familie.ba.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
    @Value("\${rolle.beslutter}")
    val BESLUTTER_ROLLE: String,
    @Value("\${rolle.saksbehandler}")
    val SAKSBEHANDLER_ROLLE: String,
    @Value("\${rolle.veileder}")
    val VEILEDER_ROLLE: String,
    @Value("\${rolle.forvalter}")
    val FORVALTER_ROLLE: String,
    @Value("\${rolle.kode6}")
    val KODE6: String,
    @Value("\${rolle.kode7}")
    val KODE7: String,
)

enum class BehandlerRolle(
    val nivå: Int,
) {
    SYSTEM(5),
    BESLUTTER(4),
    SAKSBEHANDLER(3),
    FORVALTER(2),
    VEILEDER(1),
    UKJENT(0),
}
