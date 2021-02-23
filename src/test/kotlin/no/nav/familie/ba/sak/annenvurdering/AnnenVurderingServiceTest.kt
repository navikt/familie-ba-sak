package no.nav.familie.ba.sak.annenvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.behandling.restDomene.RestAnnenVurdering
import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class AnnenVurderingServiceTest {

    private val annenVurderingRepository = mockk<AnnenVurderingRepository>(relaxed = true)

    private lateinit var annenVurderingService: AnnenVurderingService

    @BeforeEach
    fun setUp() {
        annenVurderingService = AnnenVurderingService(annenVurderingRepository = annenVurderingRepository)
    }

    @Test
    fun `Verifiser endreAnnenVurdering`() {

        every { annenVurderingRepository.findById(any()) } returns Optional.of(AnnenVurdering(resultat = Resultat.OPPFYLT,
                                                                                              type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                                                                              begrunnelse = "begrunnelse",
                                                                                              personResultat = null))
        val nyAnnenVurering = AnnenVurdering(resultat = Resultat.IKKE_OPPFYLT,
                                             type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                             begrunnelse = "begrunnelse to",
                                             personResultat = null)

        every { annenVurderingRepository.save(any()) } returns nyAnnenVurering

        annenVurderingService.endreAnnenVurdering(annenVurderingId = 123L,
                                                  RestAnnenVurdering(Resultat.IKKE_OPPFYLT,
                                                                     type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                                                     begrunnelse = "begrunnelse to"))

        verify(exactly = 1) {
            annenVurderingRepository.save(nyAnnenVurering)
        }
    }
}