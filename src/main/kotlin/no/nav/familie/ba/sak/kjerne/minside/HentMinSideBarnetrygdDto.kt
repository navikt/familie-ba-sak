package no.nav.familie.ba.sak.kjerne.minside

sealed class HentMinSideBarnetrygdDto {
    data class Suksess(
        var barnetrygd: MinSideBarnetrygd? = null,
    ) : HentMinSideBarnetrygdDto() {
        companion object {
            fun opprettFraDomene(minSideBarnetrygd: MinSideBarnetrygd?) = Suksess(minSideBarnetrygd)
        }
    }

    data class Feil(
        val feilmelding: String,
    ) : HentMinSideBarnetrygdDto()
}
