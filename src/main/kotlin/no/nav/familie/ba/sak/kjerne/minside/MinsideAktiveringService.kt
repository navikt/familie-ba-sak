package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class MinsideAktiveringService(
    private val minsideAktiveringRepository: MinsideAktiveringRepository,
) {
    fun harAktivertMinsideAktivering(aktør: Aktør): Boolean = minsideAktiveringRepository.existsByAktørAndAktivertIsTrue(aktør)

    fun aktiverMinsideAktivering(aktør: Aktør): MinsideAktivering {
        val minsideAktivering = minsideAktiveringRepository.findByAktør(aktør)
        val aktivertMinsideAktivering =
            minsideAktivering?.copy(aktivert = true) ?: MinsideAktivering(aktør = aktør, aktivert = true)
        return minsideAktiveringRepository.save(aktivertMinsideAktivering)
    }

    fun deaktiverMinsideAktivering(aktør: Aktør): MinsideAktivering {
        val minsideAktivering = minsideAktiveringRepository.findByAktør(aktør)
        val deaktivertMinsideAktivering =
            minsideAktivering?.copy(aktivert = false) ?: MinsideAktivering(aktør = aktør, aktivert = false)
        return minsideAktiveringRepository.save(deaktivertMinsideAktivering)
    }

    fun hentAktiverteMinsideAktiveringerForAktører(aktører: List<Aktør>): List<MinsideAktivering> = minsideAktiveringRepository.findAllByAktørInAndAktivertIsTrue(aktører)
}
