package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime.now

class StønadsstatistikkServiceIntegrasjonsTest(
    @Autowired private val stønadsstatistikkService: StønadsstatistikkService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val vedtakRepository: VedtakRepository,
) : AbstractSpringIntegrationTest() {
    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @BeforeEach
    fun setup() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `hentVedtakV2 får ikke sisteIverksatteBehandlingId når det finnes et utbetalingsoppdrag på behandlingen`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val barn = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling1 = behandlingRepository.save(lagBehandlingUtenId(fagsak))
        persongrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling1.id,
                lagPerson(aktør = søker, personIdent = PersonIdent(søker.aktivFødselsnummer())),
                lagPerson(aktør = barn, personIdent = PersonIdent(barn.aktivFødselsnummer()), type = PersonType.BARN, navn = PersonType.BARN.name),
            ),
        )
        vedtakRepository.saveAndFlush(lagVedtak(behandling = behandling1, vedtaksdato = now()))
        val behandling = behandling1
        tilkjentYtelseRepository.save(lagTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = "Ikke-tom streng"))

        // Act
        val vedtakDVHV2 = stønadsstatistikkService.hentVedtakV2(behandlingId = behandling.id)

        // Assert
        assertThat(vedtakDVHV2.sisteIverksatteBehandlingId).isNull()
    }

    @Test
    fun `hentVedtakV2 får sisteIverksatteBehandlingId når det ikke finnes et utbetalingsoppdrag på behandlingen`() {
        // Arrange
        val søker = aktørIdRepository.save(randomAktør())
        val barn = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandlingAvsluttet = behandlingRepository.save(lagBehandlingUtenId(fagsak, status = BehandlingStatus.AVSLUTTET, aktiv = false))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak, status = BehandlingStatus.IVERKSETTER_VEDTAK))

        persongrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                lagPerson(aktør = søker, personIdent = PersonIdent(søker.aktivFødselsnummer())),
                lagPerson(aktør = barn, personIdent = PersonIdent(barn.aktivFødselsnummer()), type = PersonType.BARN, navn = PersonType.BARN.name),
            ),
        )
        vedtakRepository.saveAndFlush(lagVedtak(behandling = behandling, vedtaksdato = now()))

        tilkjentYtelseRepository.save(lagTilkjentYtelse(behandling = behandlingAvsluttet, utbetalingsoppdrag = "Ikke-tom streng"))
        tilkjentYtelseRepository.save(lagTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = null))

        // Act
        val vedtakDVHV2 = stønadsstatistikkService.hentVedtakV2(behandlingId = behandling.id)

        // Assert
        assertThat(vedtakDVHV2.sisteIverksatteBehandlingId).isEqualTo(behandlingAvsluttet.id.toString())
    }
}
