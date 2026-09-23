package no.nav.familie.ba.sak.kjerne.logg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LoggServiceEnhetTest {
    private val loggRepository = mockk<LoggRepository>()
    private val pdlRestKlient = mockk<PdlRestKlient>()
    private val loggService = LoggService(loggRepository, pdlRestKlient)

    @Test
    fun `loggSammensattKontrollsakLagtTil skal lagre ned logg på at sammensatt kontrollsak er opprettet`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val sammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test")

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakLagtTil(sammensattKontrollsak)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakEndret skal lagre ned logg på at sammensatt kontrollsak er endret`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val oppdatertSammensattKontrollsak = SammensattKontrollsak(behandlingId = behandling.id, fritekst = "test2")

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakEndret(oppdatertSammensattKontrollsak)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_ENDRET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `loggSammensattKontrollsakFjernet skal lagre ned logg på at sammensatt kontrollsak er fjernet`() {
        // Arrange
        val behandling = lagBehandling(id = 1)

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggSammensattKontrollsakFjernet(behandlingId = behandling.id)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.SAMMENSATT_KONTROLLSAK_FJERNET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
    }

    @Test
    fun `opprettFiltreringsreglerLogg skal lagre logg om at filtreringsreglene er oppfylt`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val filtreringResultater =
            listOf(
                lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.OPPFYLT),
                lagFiltreringResultat(behandlingId = behandling.id, filtreringsregel = Filtreringsregel.Identifikator.BARN_LEVER, resultat = Resultat.OPPFYLT),
            )

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.FILTRERINGSREGLER_VURDERT)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
        assertThat(opprettetLogg.tittel).isEqualTo("Filtreringsregler gjennomført")
        assertThat(opprettetLogg.tekst).isEqualTo("Alle filtreringsreglene er oppfylt. Behandlingen fortsetter automatisk.")
    }

    @Test
    fun `opprettFiltreringsreglerLogg skal lagre logg med begrunnelsen til første filtreringsregel som ikke er oppfylt`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val filtreringResultater =
            listOf(
                lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.OPPFYLT),
                lagFiltreringResultat(
                    behandlingId = behandling.id,
                    filtreringsregel = Filtreringsregel.Identifikator.BARN_LEVER,
                    resultat = Resultat.IKKE_OPPFYLT,
                    begrunnelse = "Det er registrert dødsdato på barnet.",
                ),
                lagFiltreringResultat(
                    behandlingId = behandling.id,
                    filtreringsregel = Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR,
                    resultat = Resultat.IKKE_VURDERT,
                    begrunnelse = "Ikke vurdert",
                ),
            )

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater)

        // Assert
        assertThat(opprettetLogg.type).isEqualTo(LoggType.FILTRERINGSREGLER_VURDERT)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
        assertThat(opprettetLogg.tittel).isEqualTo("Filtreringsregler feilet")
        assertThat(opprettetLogg.tekst).isEqualTo("Behandlingen stoppet i filtreringsreglene: Det er registrert dødsdato på barnet.")
    }

    @Test
    fun `opprettFiltreringsreglerLogg skal lagre logg om at filtreringsreglene feilet når en regel ikke er vurdert`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val filtreringResultater =
            listOf(
                lagFiltreringResultat(behandlingId = behandling.id, resultat = Resultat.OPPFYLT),
                lagFiltreringResultat(
                    behandlingId = behandling.id,
                    filtreringsregel = Filtreringsregel.Identifikator.BARN_LEVER,
                    resultat = Resultat.IKKE_VURDERT,
                    begrunnelse = "Ikke vurdert",
                ),
            )

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.opprettFiltreringsreglerLogg(behandling, filtreringResultater)

        // Assert
        assertThat(opprettetLogg.tittel).isEqualTo("Filtreringsregler feilet")
        assertThat(opprettetLogg.tekst).isEqualTo("Behandlingen stoppet i filtreringsreglene: Ikke vurdert")
    }
}
