package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagFiltreringsreglerFaktaSøknad
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel.Identifikator
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FiltreringsregelEvaluatorTest {
    private val featureToggleService = mockk<FeatureToggleService>()

    private val filtreringsregelEvaluator = FiltreringsregelEvaluator(featureToggleService)

    private val ikkeOppfyltRegel1 = Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_LEVER) { false }
    private val ikkeOppfyltRegel2 = Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_ER_OVER_18_ÅR) { false }
    private val oppfyltRegel1 = Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.BARN_LEVER) { true }
    private val oppfyltRegel2 = Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.BARN_HAR_IKKE_D_NUMMER) { true }

    @Nested
    inner class FiltreringsregelEvaluator {
        @Test
        fun `skal ikke vurdere alle regler når toggle er av`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad()

            every { featureToggleService.isEnabled(FeatureToggle.VURDER_ALLE_FILTRERINGSREGLER) } returns false

            // Act
            val evalueringer = filtreringsregelEvaluator.evaluerFiltreringsregler(listOf(oppfyltRegel1, ikkeOppfyltRegel1, oppfyltRegel2, ikkeOppfyltRegel2), fakta)

            // Assert
            assertThat(evalueringer.map { it.resultat }).containsExactly(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT, Resultat.IKKE_VURDERT, Resultat.IKKE_VURDERT)
        }

        @Test
        fun `skal vurdere alle regler når toggle er på`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad()

            every { featureToggleService.isEnabled(FeatureToggle.VURDER_ALLE_FILTRERINGSREGLER) } returns true

            // Act
            val evalueringer = filtreringsregelEvaluator.evaluerFiltreringsregler(listOf(oppfyltRegel1, ikkeOppfyltRegel1, oppfyltRegel2, ikkeOppfyltRegel2), fakta)

            // Assert
            assertThat(evalueringer.map { it.resultat }).containsExactly(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT, Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT)
        }
    }
}
