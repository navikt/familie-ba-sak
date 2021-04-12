package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.*

val nbLocale = Locale("nb", "Norway")

object Utils {
    fun slåSammen(values: List<String>): String = Regex("(.*),").replace(values.joinToString(", "), "$1 og")

    fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

    // TODO: Tilpasset fastsettelse av BehandlingResultatType inntil støtte for delvis innvilgelse.
    //  Fastsettelse nedenfor løser enkelte steder generering av utbetalingsoppdrag til økonomi, men det vil fortsatt se rart ut
    //  frontend og i database vil det bli satt opphørsdato på TilkjentYtelse-nivå frem til støtte for delvis.
    fun midlertidigUtledBehandlingResultatType(hentetBehandlingResultat: BehandlingResultat) =
            when {
                (hentetBehandlingResultat == BehandlingResultat.OPPHØRT) -> BehandlingResultat.DELVIS_INNVILGET
                else -> hentetBehandlingResultat
            }

    fun hentPropertyFraMaven(key: String): String? {
        val reader = MavenXpp3Reader()
        val model: Model = if (File("pom.xml").exists()) reader.read(FileReader("pom.xml")) else reader.read(
                InputStreamReader(
                        ClassPathResource(
                                "META-INF/maven/no.nav.familie.ba.sak/familie-ba-sak/pom.xml"
                        ).inputStream
                )
        )

        return model.properties[key]?.toString()
    }
}