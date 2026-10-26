package no.nav.familie.ba.sak.kjerne.logg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFiltreringResultat
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak.SammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
    fun `opprettFiltreringsreglerLogg skal lagre logg med begrunnelsene til filtreringsregler som ikke er oppfylt og utelate ikke vurderte`() {
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
        assertThat(opprettetLogg.tekst).isEqualTo(
            "Behandlingen stoppet i filtreringsreglene: Det er registrert dødsdato på barnet.",
        )
    }

    @Test
    fun `loggEndretUtbetalingAndelLagtTil skal lagre logg når siste logg på behandlingen er av en annen type`() {
        // Arrange
        val behandlingId = 1L
        every { loggRepository.hentLoggForBehandling(behandlingId) } returns
            listOf(Logg(behandlingId = behandlingId, type = LoggType.ENDRET_UTBETALING_FJERNET, rolle = BehandlerRolle.SAKSBEHANDLER))
        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg = loggService.loggEndretUtbetalingAndelLagtTil(behandlingId)

        // Assert
        assertThat(opprettetLogg?.type).isEqualTo(LoggType.ENDRET_UTBETALING_LAGT_TIL)
        assertThat(opprettetLogg?.tittel).isEqualTo("Endret utbetalingsperiode lagt til")
        verify(exactly = 1) { loggRepository.save(any()) }
    }

    @Test
    fun `loggEndretUtbetalingAndelLagtTil skal ikke lagre logg når siste logg på behandlingen er av samme type`() {
        // Arrange
        val behandlingId = 1L
        every { loggRepository.hentLoggForBehandling(behandlingId) } returns
            listOf(Logg(behandlingId = behandlingId, type = LoggType.ENDRET_UTBETALING_LAGT_TIL, rolle = BehandlerRolle.SAKSBEHANDLER))

        // Act
        val opprettetLogg = loggService.loggEndretUtbetalingAndelLagtTil(behandlingId)

        // Assert
        assertThat(opprettetLogg).isNull()
        verify(exactly = 0) { loggRepository.save(any()) }
    }

    @Test
    fun `opprettSøknadstidspunktEndretLogg skal lagre ett samlet innslag med kun endrede personer, søker først og barn sortert på fødselsdato`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(1990, 1, 1))
        val eldsteBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2018, 3, 4))
        val yngsteBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2021, 5, 6))
        val uendretBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 7, 8))
        val fjernetBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 9, 10))

        every { loggRepository.save(any()) } returnsArgument 0

        // Act
        val opprettetLogg =
            loggService.opprettSøknadstidspunktEndretLogg(
                behandling = behandling,
                personer = setOf(yngsteBarn, fjernetBarn, uendretBarn, eldsteBarn, søker),
                forrigeSøknadstidspunktPerAktør =
                    mapOf(
                        søker.aktør to LocalDate.of(2025, 2, 1),
                        eldsteBarn.aktør to LocalDate.of(2025, 2, 1),
                        uendretBarn.aktør to LocalDate.of(2025, 2, 1),
                        fjernetBarn.aktør to LocalDate.of(2025, 2, 1),
                    ),
                nyttSøknadstidspunktPerAktør =
                    mapOf(
                        søker.aktør to LocalDate.of(2025, 3, 1),
                        eldsteBarn.aktør to LocalDate.of(2025, 3, 1),
                        yngsteBarn.aktør to LocalDate.of(2025, 3, 1),
                        uendretBarn.aktør to LocalDate.of(2025, 2, 1),
                    ),
            )

        // Assert
        assertThat(opprettetLogg).isNotNull
        assertThat(opprettetLogg!!.type).isEqualTo(LoggType.SØKNADSTIDSPUNKT_ENDRET)
        assertThat(opprettetLogg.behandlingId).isEqualTo(behandling.id)
        assertThat(opprettetLogg.tittel).isEqualTo("Søknadstidspunkt endret")
        assertThat(opprettetLogg.tekst).isEqualTo("Søker: 01.03.25, Barn 04.03.18: 01.03.25, Barn 10.09.20: fjernet, Barn 06.05.21: 01.03.25")
        verify(exactly = 1) { loggRepository.save(any()) }
    }

    @Test
    fun `opprettSøknadstidspunktEndretLogg skal ikke lagre innslag når ingen søknadstidspunkt er endret`() {
        // Arrange
        val behandling = lagBehandling(id = 1)
        val barn = lagPerson(type = PersonType.BARN)
        val søknadstidspunkt = mapOf(barn.aktør to LocalDate.of(2025, 2, 1))

        // Act
        val opprettetLogg =
            loggService.opprettSøknadstidspunktEndretLogg(
                behandling = behandling,
                personer = setOf(barn),
                forrigeSøknadstidspunktPerAktør = søknadstidspunkt,
                nyttSøknadstidspunktPerAktør = søknadstidspunkt,
            )

        // Assert
        assertThat(opprettetLogg).isNull()
        verify(exactly = 0) { loggRepository.save(any()) }
    }
}
