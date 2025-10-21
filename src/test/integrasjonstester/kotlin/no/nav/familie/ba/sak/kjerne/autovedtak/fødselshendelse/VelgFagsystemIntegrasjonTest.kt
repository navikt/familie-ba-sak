package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.fake.FakeInfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.settPersonInfoStatsborgerskap
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.apache.kafka.shaded.com.google.protobuf.LazyStringArrayList.emptyList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.collections.emptyList

class VelgFagsystemIntegrasjonTest(
    @Autowired val stegService: StegService,
    @Autowired val persongrunnlagService: PersongrunnlagService,
    @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired val velgFagSystemService: VelgFagSystemService,
    @Autowired val fakeInfotrygdBarnetrygdKlient: FakeInfotrygdBarnetrygdKlient,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        fakeInfotrygdBarnetrygdKlient.leggTilLøpendeSakIInfotrygd(søkerFnr, emptyList(), true)
        assertEquals(true, velgFagSystemService.morEllerBarnHarLøpendeSakIInfotrygd(søkerFnr, emptyList()))
    }

    @Test
    fun `skal IKKE velge ba-sak når mor har stønadhistorikk i Infotrygd`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))
        val fagsystemUtfall = FagsystemUtfall.SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER

        fakeInfotrygdBarnetrygdKlient.leggTilStønaderIInfotrygd(
            søkerFnr,
            emptyList(),
            InfotrygdSøkResponse<Stønad>(
                listOf(Stønad(opphørtFom = "012020")),
                emptyList<Stønad>(),
            ),
        )

        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_INFOTRYGD, fagsystemRegelVurdering)
        assertEquals(fagsystemUtfall, faktiskFagsystemUtfall)
    }

    @Test
    fun `skal IKKE velge ba-sak når mor er EØS borger`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))

        settPersonInfoStatsborgerskap(
            søkerFnr,
            Statsborgerskap(
                land = "POL",
                gyldigFraOgMed = LocalDate.now().minusYears(2),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            ),
        )
        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fagsystemRegelVurdering)
        assertEquals(FagsystemUtfall.STØTTET_I_BA_SAK, faktiskFagsystemUtfall)
    }

    @Test
    fun `skal velge ba-sak når mor er tredjelandsborger`() {
        val søkerFnr = leggTilPersonInfo(randomSøkerFødselsdato())
        val barnFnr = leggTilPersonInfo(randomBarnFødselsdato())
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val fagsystemUtfall = FagsystemUtfall.STØTTET_I_BA_SAK

        settPersonInfoStatsborgerskap(
            søkerFnr,
            Statsborgerskap(
                land = "USA",
                gyldigFraOgMed = LocalDate.now().minusYears(2),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            ),
        )

        val (fagsystemRegelVurdering, faktiskFagsystemUtfall) = velgFagSystemService.velgFagsystem(nyBehandling)
        assertEquals(FagsystemRegelVurdering.SEND_TIL_BA, fagsystemRegelVurdering)
        assertEquals(fagsystemUtfall, faktiskFagsystemUtfall)
    }
}
