package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkService(
    private val integrasjonKlient: IntegrasjonKlient,
) {
    fun hentLand(landkode: String): String = integrasjonKlient.hentLand(landkode)

    fun hentPoststed(postnummer: String?): String? =
        integrasjonKlient
            .hentPoststeder()
            .betydninger[postnummer]
            ?.firstOrNull()
            ?.beskrivelser[KodeverkSpråk.BOKMÅL.kode]
            ?.term
            ?.storForbokstav()

    fun henteEøsMedlemskapsPerioderForValgtLand(land: String): List<BetydningDto> =
        integrasjonKlient.hentAlleEØSLand().betydninger[land].apply {
            if (land == STORBRITTANNIA_LANDKODE) {
                this?.map {
                    BetydningDto(
                        gyldigFra = it.gyldigFra,
                        gyldigTil = if (it.gyldigFra < BREXIT_OVERGANGSORDNING_TOM_DATO) BREXIT_OVERGANGSORDNING_TOM_DATO else it.gyldigTil,
                        beskrivelser = it.beskrivelser,
                    )
                }
            }
        } ?: emptyList()

    fun hentEøsMedlemskapsTidslinje(land: String): Tidslinje<Boolean> =
        henteEøsMedlemskapsPerioderForValgtLand(land)
            .map { betydningsDto ->
                Periode(
                    verdi = true,
                    fom = betydningsDto.gyldigFra,
                    tom =
                        betydningsDto.gyldigTil.let {
                            if (it.isAfter(LocalDate.of(5000, 1, 1))) {
                                null
                            } else {
                                it
                            }
                        },
                )
            }.tilTidslinje()

    fun hentLandkoderISO2() = integrasjonKlient.hentLandkoderISO2()

    companion object {
        // Kodeverk har satt tom-dato til 1. januar 2020, men pga overgangsordningen er dette datoen i praksis
        val BREXIT_OVERGANGSORDNING_TOM_DATO = LocalDate.of(2020, 12, 31)
        val STORBRITANNIA_LANDKODE = "GBR"
    }
}
