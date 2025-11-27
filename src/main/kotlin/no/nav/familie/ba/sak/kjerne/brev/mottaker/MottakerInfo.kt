package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType

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
    override val navn: String,
) : MottakerInfo

fun MottakerInfo.tilAvsenderMottaker(): AvsenderMottaker? =
    when (this) {
        is FullmektigEllerVerge, is Dødsbo -> {
            AvsenderMottaker(
                navn = navn,
                id = null,
                idType = null,
            )
        }

        is Institusjon -> {
            AvsenderMottaker(
                idType = AvsenderMottakerIdType.ORGNR,
                id = orgNummer,
                navn = navn,
            )
        }

        // Trenger ikke overstyres når mottaker er bruker
        is Bruker, is BrukerMedUtenlandskAdresse -> {
            null
        }
    }

data class ManuellAdresseInfo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)
