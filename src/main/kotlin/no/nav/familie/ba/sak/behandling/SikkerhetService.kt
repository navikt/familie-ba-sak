package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo

class SikkerhetService {
    companion object {
        fun finnStrengesteDiskresjonskode(personer: List<Personinfo>): String? {
            return personer.fold(null, fun(kode: String?, personinfo: Personinfo): String? {
                return when {
                    kode == Diskresjonskode.KODE6.kode || personinfo.diskresjonskode == Diskresjonskode.KODE6.kode -> {
                        Diskresjonskode.KODE6.kode
                    }
                    kode == Diskresjonskode.KODE7.kode || personinfo.diskresjonskode == Diskresjonskode.KODE7.kode -> {
                        Diskresjonskode.KODE7.kode
                    }
                    else -> null
                }
            })
        }
    }
}

enum class Diskresjonskode(val kode: String) {
    KODE6("SPSF"),
    KODE7("SPFO")
}