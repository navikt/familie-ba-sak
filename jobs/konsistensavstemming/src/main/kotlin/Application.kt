package no.nav.familie-ba-sak-konsistensavstemming

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID
import kotlin.system.exitProcess

val logger: Logger = LoggerFactory.getLogger("KonsistensavstemmingsJob")
fun main() {
    logger.info("konsistensavstemming startet")
    val env = System.getenv()
    val dbUser = env.getValue("POSTGRES_GCP_DB_USER")
    val dbPassword = env.getValue("POSTGRES_GCP_DB_PASSWORD")
    val dbHost: String = getEnvVar("POSTGRES_GCP_FAMILIE_BA_SAK_HOST")
    val dbPort: String = getEnvVar("POSTGRES_GCP_FAMILIE_BA_SAK_PORT")
    val cloudSqlInstance: String = getEnvVar("CLOUD_SQL_INSTANCE")

    logger.info("Konfig lest, oppretter forbindelse til DB")

    val jdbcUrl = "jdbc:postgresql://${dbHost}:${dbPort}/familie-ba-sak?user=${dbUser}&password=${dbPasswoed}

    val connection = DriverManager.getConnection(jdbcUrl)

    logger.info("Connection er oppe og kjører" + connection.isValid(0))


    logger.info("konsistensavstemming ferdig")
    exitProcess(0)
}
