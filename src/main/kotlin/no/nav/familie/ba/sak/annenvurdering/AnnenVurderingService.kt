package no.nav.familie.ba.sak.annenvurdering

import no.nav.familie.ba.sak.behandling.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnenVurderingService(
        private val annenVurderingRepository: AnnenVurderingRepository,
        private val loggService: LoggService
) {

    fun hent(personResultat: PersonResultat, andreVurderingerType: AnnenVurderingType): AnnenVurdering? =
            annenVurderingRepository.findBy(personResultat = personResultat,
                                            type = andreVurderingerType)

    fun hent(annenVurderingId: Long): AnnenVurdering = annenVurderingRepository.findById(annenVurderingId)
            .orElseThrow { error("Annen vurdering med id $annenVurderingId finnes ikke i db") }

    @Transactional
    fun endreAnnenVurdering(annenVurderingId: Long, restAnnenVurdering: RestAnnenVurdering) {
        hent(annenVurderingId = annenVurderingId).let {
            annenVurderingRepository.save(it.also {
                it.resultat = restAnnenVurdering.resultat
                it.begrunnelse = restAnnenVurdering.begrunnelse
                it.type = restAnnenVurdering.type
            })
        }
    }
}

fun PersonResultat.leggTilBlankAnnenVurdering(andreVurderingerType: AnnenVurderingType) {
    this.andreVurderinger.add(AnnenVurdering(personResultat = this,
                                             resultat = Resultat.IKKE_VURDERT,
                                             type = andreVurderingerType,
                                             begrunnelse = null))
}