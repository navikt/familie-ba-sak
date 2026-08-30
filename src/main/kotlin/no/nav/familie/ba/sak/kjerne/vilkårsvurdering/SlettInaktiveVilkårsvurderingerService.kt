package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SlettInaktiveVilkårsvurderingerService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    /**
     * Sletter én batch med inaktive vilkårsvurderinger (med id større enn [etterId], i stigende rekkefølge)
     * og tilhørende person_resultat, vilkar_resultat og annen_vurdering. Barna slettes før foreldrene siden
     * fremmednøklene ikke har ON DELETE CASCADE.
     *
     * Hver batch kjøres i sin egen transaksjon slik at låser og WAL holdes små, og allerede slettede
     * batcher ikke rulles tilbake om en senere batch feiler. Kalleren sender inn siste slettede id som
     * [etterId] i neste kall slik at allerede gjennomgåtte rader ikke skannes på nytt.
     *
     * @return id-ene til vilkårsvurderingene som ble slettet, i stigende rekkefølge (tom liste når det ikke er flere)
     */
    @Transactional
    fun slettBatchMedInaktiveVilkårsvurderinger(
        etterId: Long,
        batchStørrelse: Int,
    ): List<Long> {
        val vilkårsvurderingIder = vilkårsvurderingRepository.finnIderForInaktiveVilkårsvurderinger(etterId, PageRequest.of(0, batchStørrelse))
        if (vilkårsvurderingIder.isEmpty()) return emptyList()

        vilkårsvurderingRepository.slettAndreVurderingerForVilkårsvurderinger(vilkårsvurderingIder)
        vilkårsvurderingRepository.slettVilkårResultaterForVilkårsvurderinger(vilkårsvurderingIder)
        vilkårsvurderingRepository.slettPersonResultaterForVilkårsvurderinger(vilkårsvurderingIder)
        vilkårsvurderingRepository.slettVilkårsvurderinger(vilkårsvurderingIder)
        return vilkårsvurderingIder
    }
}
