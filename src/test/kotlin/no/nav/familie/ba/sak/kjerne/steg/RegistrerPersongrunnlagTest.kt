package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RegistrerPersongrunnlagTest(
    @Autowired
    private val stegService: StegService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val personopplysningerService: PersonopplysningerService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService
) : AbstractSpringIntegrationTest(personopplysningerService) {

    @BeforeAll
    fun truncate() {
        databaseCleanupService.truncate()
    }

    @Test
    @Tag("integration")
    fun `Legg til personer på behandling`() {
        val morId = randomFnr()
        val barn1Id = randomFnr()
        val barn2Id = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling1 =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        stegService.håndterPersongrunnlag(
            behandling = behandling1,
            registrerPersongrunnlagDTO = RegistrerPersongrunnlagDTO(
                ident = morId,
                barnasIdenter = listOf(barn1Id, barn2Id)
            )
        )

        val grunnlag1 = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling1.id)

        Assertions.assertEquals(3, grunnlag1!!.personer.size)
        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == barn1Id })
        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == barn2Id })
        Assertions.assertEquals(2, grunnlag1.personer.first { it.type == PersonType.SØKER }.sivilstander.size)

        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == barn1Id })
    }

    @Test
    @Tag("integration")
    fun `Legg til barn på eksisterende behandling`() {
        val morId = randomFnr()
        val barn1Id = randomFnr()
        val barn2Id = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling1 =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        stegService.håndterPersongrunnlag(
            behandling = behandling1,
            registrerPersongrunnlagDTO = RegistrerPersongrunnlagDTO(
                ident = morId,
                barnasIdenter = listOf(barn1Id)
            )
        )

        val grunnlag1 = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling1.id)

        Assertions.assertEquals(2, grunnlag1!!.personer.size)
        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag1.personer.any { it.personIdent.ident == barn1Id })

        stegService.håndterPersongrunnlag(
            behandling = behandling1,
            registrerPersongrunnlagDTO = RegistrerPersongrunnlagDTO(
                ident = morId,
                barnasIdenter = listOf(
                    barn1Id,
                    barn2Id
                )
            )
        )
        val grunnlag2 = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling1.id)

        Assertions.assertEquals(3, grunnlag2!!.personer.size)
        Assertions.assertTrue(grunnlag2.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag2.personer.any { it.personIdent.ident == barn1Id })
        Assertions.assertTrue(grunnlag2.personer.any { it.personIdent.ident == barn2Id })

        // Skal ikke føre til flere personer på persongrunnlaget
        stegService.håndterPersongrunnlag(
            behandling = behandling1,
            registrerPersongrunnlagDTO = RegistrerPersongrunnlagDTO(
                ident = morId,
                barnasIdenter = listOf(
                    barn1Id,
                    barn2Id
                )
            )
        )

        val grunnlag3 = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling1.id)

        Assertions.assertEquals(3, grunnlag3!!.personer.size)
        Assertions.assertTrue(grunnlag3.personer.any { it.personIdent.ident == morId })
        Assertions.assertTrue(grunnlag3.personer.any { it.personIdent.ident == barn1Id })
        Assertions.assertTrue(grunnlag3.personer.any { it.personIdent.ident == barn2Id })
    }
}
