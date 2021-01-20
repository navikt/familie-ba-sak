package no.nav.familie.ba.sak.brev

import org.springframework.stereotype.Service

@Service
class FamilieBrevService(val familieBrevKlient: FamilieBrevKlient
) {
    fun genererBrev(målform: String, malnavn: String, body: Any): ByteArray {
        return familieBrevKlient.genererBrev(målform, malnavn, body)
    }
}
