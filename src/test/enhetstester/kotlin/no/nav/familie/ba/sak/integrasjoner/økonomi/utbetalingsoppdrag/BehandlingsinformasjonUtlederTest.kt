package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagInstitusjon
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class BehandlingsinformasjonUtlederTest {
    private val clock: Clock =
        Clock.fixed(
            Instant.parse("2024-11-01T10:00:00Z"),
            ZoneId.of("Europe/Oslo"),
        )
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder = mockk()
    private val behandlingsinformasjonUtleder: BehandlingsinformasjonUtleder =
        BehandlingsinformasjonUtleder(
            endretMigreringsdatoUtleder,
            TestClockProvider(clock),
        )

    @Test
    fun `skal utlede mininal behandlingsinformasjon`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor vedtaksdato er satt til vedtaksdatoen fra vedtaket`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = LocalDateTime.now(clock),
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(vedtak.vedtaksdato!!.toLocalDate())
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørAlleKjederFra er null da forrige tilkjent ytelse ikke er null`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
            )

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.now(clock))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørAlleKjederFra er null da forrige tilkjent ytelse ikke er null og siste andel per kjede ikke er tom`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
            )

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                fom = YearMonth.now(clock),
                tom = YearMonth.now(clock),
            )

        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("1", YtelsetypeBA.ORDINÆR_BARNETRYGD) to andelTilkjentYtelse.tilAndelDataLongId(),
            )

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = sisteAndelPerKjede,
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.now(clock))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørAlleKjederFra er satt til endret migreringsdato`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse =
            lagTilkjentYtelse(
                behandling = behandling,
            )

        val lagAndelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                fom = YearMonth.now(clock),
                tom = YearMonth.now(clock),
            )

        val sisteAndelPerKjede =
            mapOf(
                IdentOgType("1", YtelsetypeBA.ORDINÆR_BARNETRYGD) to lagAndelTilkjentYtelse.tilAndelDataLongId(),
            )

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns YearMonth.now(clock)

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = sisteAndelPerKjede,
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.now(clock))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isEqualTo(YearMonth.now(clock))
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor utbetalesTil settes til fagsak aktør for NORMAL fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor utbetalesTil settes til fagsak aktør for BARN_ENSLIG_MINDREÅRIG fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.BARN_ENSLIG_MINDREÅRIG,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor utbetalesTil settes til institusjon for INSTITUSJON fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"

        val institusjon =
            lagInstitusjon(
                tssEksternId = "eksternId",
            )

        val fagsak =
            lagFagsak(
                type = FagsakType.INSTITUSJON,
                institusjon = institusjon,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(institusjon.tssEksternId!!)
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal kaste exception om institusjon er null for INSTITUSJON fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.INSTITUSJON,
                institusjon = null,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act & assert
        val exception =
            assertThrows<Feil> {
                behandlingsinformasjonUtleder.utled(
                    saksbehandlerId = saksbehandlerId,
                    vedtak = vedtak,
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                    sisteAndelPerKjede = mapOf(),
                    false,
                )
            }
        assertThat(exception.message).isEqualTo("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
    }

    @Test
    fun `skal kaste exception om tssEksternId på institusjon er null for INSTITUSJON fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"

        val institusjon =
            lagInstitusjon(
                tssEksternId = null,
            )

        val fagsak =
            lagFagsak(
                type = FagsakType.INSTITUSJON,
                institusjon = institusjon,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act & assert
        val exception =
            assertThrows<Feil> {
                behandlingsinformasjonUtleder.utled(
                    saksbehandlerId = saksbehandlerId,
                    vedtak = vedtak,
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                    sisteAndelPerKjede = mapOf(),
                    false,
                )
            }
        assertThat(exception.message).isEqualTo("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørKjederFraFørsteUtbetaling er satt til false da en endret migrereingsdato ikke er null`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns YearMonth.now(clock)

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor utbetalesTil settes til søker for SKJERMET_BARN fagsaktype`() {
        // Arrange
        val saksbehandlerId = "123"
        val søker = randomAktør()

        val skjermetBarnSøker =
            SkjermetBarnSøker(aktør = søker)

        val fagsak =
            lagFagsak(
                type = FagsakType.SKJERMET_BARN,
                skjermetBarnSøker = skjermetBarnSøker,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(søker.aktivFødselsnummer())
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal kaste feil under utleding av behandlingsinformasjon dersom fagsak type er skjermet barn og det ikke er lagret ned søker`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.SKJERMET_BARN,
                skjermetBarnSøker = null,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act && Assert
        val feilmelding =
            assertThrows<Feil> {
                behandlingsinformasjonUtleder.utled(
                    saksbehandlerId = saksbehandlerId,
                    vedtak = vedtak,
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                    sisteAndelPerKjede = mapOf(),
                    false,
                )
            }

        assertThat(feilmelding.message).isEqualTo("Barnetrygd skal utbetales til søker av barnet, men søker er ikke registrert på fagsak 1")
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørKjederFraFørsteUtbetaling er satt til false da en endret migrereingsdato er null og det ikke er for simulering`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                false,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isFalse()
    }

    @Test
    fun `skal utlede behandlingsinformasjon hvor opphørKjederFraFørsteUtbetaling er satt til true da en endret migrereingsdato er null og det er for simulering`() {
        // Arrange
        val saksbehandlerId = "123"

        val fagsak =
            lagFagsak(
                type = FagsakType.NORMAL,
            )

        val behandling =
            lagBehandling(
                fagsak = fagsak,
            )

        val vedtak =
            lagVedtak(
                behandling = behandling,
                vedtaksdato = null,
            )

        val forrigeTilkjentYtelse = null

        every {
            endretMigreringsdatoUtleder.utled(vedtak.behandling.fagsak, forrigeTilkjentYtelse)
        } returns null

        // Act
        val behandlingsinformasjon =
            behandlingsinformasjonUtleder.utled(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                sisteAndelPerKjede = mapOf(),
                true,
            )

        // Assert
        assertThat(behandlingsinformasjon.saksbehandlerId).isEqualTo(saksbehandlerId)
        assertThat(behandlingsinformasjon.behandlingId).isEqualTo(vedtak.behandling.id.toString())
        assertThat(behandlingsinformasjon.eksternBehandlingId).isEqualTo(vedtak.behandling.id)
        assertThat(behandlingsinformasjon.eksternFagsakId).isEqualTo(vedtak.behandling.fagsak.id)
        assertThat(behandlingsinformasjon.fagsystem).isEqualTo(FagsystemBA.BARNETRYGD)
        assertThat(behandlingsinformasjon.personIdent).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.vedtaksdato).isEqualTo(LocalDate.of(2024, 11, 1))
        assertThat(behandlingsinformasjon.opphørAlleKjederFra).isNull()
        assertThat(behandlingsinformasjon.utbetalesTil).isEqualTo(
            vedtak.behandling.fagsak.aktør
                .aktivFødselsnummer(),
        )
        assertThat(behandlingsinformasjon.opphørKjederFraFørsteUtbetaling).isTrue()
    }
}
