package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.junit.jupiter.api.Test

class PreutfyllVilkårServiceTest {
    private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService = mockk()
    private val preutfyllBorMedSøkerService: PreutfyllBorMedSøkerService = mockk()
    private val preutfyllBosattIRiketService: PreutfyllBosattIRiketService = mockk()
    private val preutfyllBosattIRiketForFødselshendelserService: PreutfyllBosattIRiketForFødselshendelserService = mockk()
    private val gammelPreutfyllBosattIRiketService: GammelPreutfyllBosattIRiketService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    val preutfyllVilkårService =
        PreutfyllVilkårService(
            preutfyllLovligOppholdService,
            preutfyllBorMedSøkerService,
            preutfyllBosattIRiketService,
            preutfyllBosattIRiketForFødselshendelserService,
            gammelPreutfyllBosattIRiketService,
            persongrunnlagService,
            featureToggleService,
        )

    @Test
    fun `Skal ikke kjøre noe preutfylling for behandlinger i fagsak type skjermet barn`() {
        // Arrange
        val behandling =
            lagBehandling(
                fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN),
            )

        val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)

        // Act
        preutfyllVilkårService.preutfyllVilkår(vilkårsvurdering = vilkårsvurdering)

        // Assert

        verify(exactly = 0) { preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering) }
        verify(exactly = 0) { preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering) }
        verify(exactly = 0) { preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering) }
    }
}
