package no.nav.familie.ba.sak.andreopplysninger

import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import org.springframework.stereotype.Service

@Service
class AndreVurderingerService(
        private val andreVurderingerRepository: AndreVurderingerRepository,
        private val loggService: LoggService
) {

    fun hent(behandlingId: Long, personResultatId: Long, andreVurderingerType: AndreVurderingerType): AndreVurderinger? =
            andreVurderingerRepository.findBy(behandlingId = behandlingId,
                                              personResultatId = personResultatId,
                                              type = andreVurderingerType.toString())

    fun lagreBlankAndreVurderinger(behandlingId: Long, personResultatId: Long, andreVurderingerType: AndreVurderingerType) {
        val lagretAndreVurderinger = hent(behandlingId = behandlingId,
                                          personResultatId = personResultatId,
                                          andreVurderingerType = andreVurderingerType)

        if (lagretAndreVurderinger == null) {
            andreVurderingerRepository.save(AndreVurderinger(behandlingId = behandlingId,
                                                             personResultatId = personResultatId,
                                                             resultat = Resultat.IKKE_VURDERT,
                                                             type = AndreVurderingerType.OPPLYSNINGSPLIKT))
        } else {
            andreVurderingerRepository.save(lagretAndreVurderinger.also {
                it.resultat = Resultat.IKKE_VURDERT
                it.begrunnelse = null
            })
        }
    }

    fun oppdaterAndreVurderinger(behandlingId: Long,
                                 personResultatId: Long,
                                 andreVurderingerType: AndreVurderingerType,
                                 resultat: Resultat,
                                 begrunnelse: String?) {
        val lagretAndreVurderinger = hent(behandlingId = behandlingId,
                                          personResultatId = personResultatId,
                                          andreVurderingerType = andreVurderingerType)
                                     ?: error("Kunne ikke oppdatere andrevurderinger fordi andrevurderinger mangler på personresultat")

        andreVurderingerRepository.save(lagretAndreVurderinger.also {
            it.resultat = resultat
            it.begrunnelse = begrunnelse
        })
        /*loggService.opprettAndreVurderingerEndret(behandlingId = behandlingId,
                                                  endring = true,
                                                  type = andreVurderingerType,
                                                  resultat = resultat,
                                                  begrunnelse = begrunnelse)*/
    }
}