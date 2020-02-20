package no.nav.familie.ba.sak.auditlogger

import org.slf4j.LoggerFactory

private const val SPACE_SEPARATOR = ' '

object AuditLogger {
    fun log(clazz: Class<Any>, sporingsdata: Sporingsdata, type: AuditLoggerType, action: AuditLoggerAction) {
        val msg: StringBuilder = StringBuilder()
                .append("action=").append(action).append(SPACE_SEPARATOR)
                .append("actionType=").append(type)
                .append(SPACE_SEPARATOR)

        sporingsdata.verdier.map {
            msg.append(it.key).append('=').append(it.value)
                    .append(SPACE_SEPARATOR)
        }

        val sanitizedMsg: String = msg.toString().replace("(\\r|\\n)".toRegex(), "").trim()
        LoggerFactory.getLogger("auditLogger" + "." + clazz.name).info(sanitizedMsg)
    }

    fun logLesFagsak(clazz: Class<Any>, fagsakId: Long, ansvarligSaksbehandler: String) {
        log(clazz,
            Sporingsdata(verdier = mapOf(SporingsloggId.FAGSAK_ID to fagsakId.toString(),
                                         SporingsloggId.ANSVALIG_SAKSBEHANDLER to ansvarligSaksbehandler)),
            AuditLoggerType.READ,
            AuditLoggerAction.FAGSAK)
    }
}

data class Sporingsdata(
        val verdier: Map<SporingsloggId, String>
)

enum class SporingsloggId {
    FAGSAK_ID,
    ANSVALIG_SAKSBEHANDLER,
}

enum class AuditLoggerType {
    READ,
    UPDATE,
    CREATE,
    DELETE,
}

enum class AuditLoggerAction {
    FAGSAK
}