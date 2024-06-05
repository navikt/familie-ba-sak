package no.nav.familie.ba.sak.kjerne.steg.domene

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.prosessering.domene.Task

data class JournalførVedtaksbrevDTO(val vedtakId: Long, val task: Task)


sealed interface MottakerInfo {
    val navn: String
        get() = ""
    val manuellAdresseInfo: ManuellAdresseInfo?
        get() = null
}

data object Bruker : MottakerInfo

class BrukerMedUtenlandskAdresse(
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

class FullmektigEllerVerge(
    override val navn: String,
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

class Dødsbo(
    override val navn: String,
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

data class Institusjon(
    val orgNummer: String,
    val institusjonNavn: String,
) : MottakerInfo

fun MottakerInfo.tilAvsenderMottaker(): AvsenderMottaker? {
    return when (this) {
        is FullmektigEllerVerge, is Dødsbo ->
            AvsenderMottaker(
                navn = navn,
                id = null,
                idType = null,
            )
        is Institusjon -> AvsenderMottaker(
            idType = BrukerIdType.ORGNR,
            id = orgNummer,
            navn = institusjonNavn, // TODO sjekk om denne settes i frontend
        )
        // Trenger ikke overstyres når mottaker er bruker
        is Bruker, is BrukerMedUtenlandskAdresse -> null
    }
}

data class ManuellAdresseInfo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)
