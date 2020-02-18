package no.nav.familie.ba.sak.behandling

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.time.LocalDate

class ArbeidsfordelingServiceTest {
    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @MockK
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @MockK
    lateinit var personopplysningGrunnlag: PersonopplysningGrunnlag

    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    lateinit var behandling: Behandling

    lateinit var finnStrengesteDiskresjonskode: Method

    val identBarn = "12345"
    val søker = Personinfo(LocalDate.now(), null, null)
    val søkerKode7 = Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)
    val søkerKode6 = Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        arbeidsfordelingService = ArbeidsfordelingService(behandlingRepository, personopplysningGrunnlagRepository, integrasjonTjeneste)
        val fagsak = Fagsak(personIdent = PersonIdent(""))
        behandling = Behandling(
                fagsak = fagsak,
                journalpostID = "",
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NATIONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR
        )

        finnStrengesteDiskresjonskode = arbeidsfordelingService.javaClass.getDeclaredMethod("finnStrengesteDiskresjonskode", Personinfo::class.java, Behandling::class.java)
        finnStrengesteDiskresjonskode.trySetAccessible()

        every {
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(any())
        } returns personopplysningGrunnlag

        every {
            personopplysningGrunnlag.barna
        } answers {
            listOf(Person(null, PersonType.BARN, null, PersonIdent(identBarn), personopplysningGrunnlag))
        }
    }

    @Test
    fun `søker har ingen diskresjonskode, barn har ingen diskresjonskode - skal gi null`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, null)

        assertEquals(null, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søker, behandling))
    }

    @Test
    fun `søker har ingen diskresjonskode, barn er kode 7 - skal gi kode 7`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)

        assertEquals(Diskresjonskode.KODE7.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søker, behandling))
    }

    @Test
    fun `søker har ingen diskresjonskode, barn er kode 6 - skal gi kode 6`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søker, behandling))
    }

    @Test
    fun `søker er kode 7, barn har ingen diskresjonskode - skal gi kode 7`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, null)

        assertEquals(Diskresjonskode.KODE7.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode7, behandling))
    }

    @Test
    fun `søker er kode 7, barn er kode 7 - skal gi kode 7`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)

        assertEquals(Diskresjonskode.KODE7.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode7, behandling))
    }

    @Test
    fun `søker er kode 7, barn er kode 6 - skal gi kode 6`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode7, behandling))
    }

    @Test
    fun `søker er kode 6, barn har ingen diskresjonskode - skal gi kode 6`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, null)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode6, behandling))
    }

    @Test
    fun `søker er kode 6, barn er kode 7 - skal gi kode 6`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode6, behandling))
    }

    @Test
    fun `søker er kode 6, barn er kode 6 - skal gi kode 6`() {
        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerKode6, behandling))
    }

    @Test
    fun `et barn er kode 6 blant barn som ikke er kode 6 - skal gi kode 6`() {
        every {
            personopplysningGrunnlag.barna
        } answers {
            listOf(
                    Person(null, PersonType.BARN, null, PersonIdent("1"), personopplysningGrunnlag),
                    Person(null, PersonType.BARN, null, PersonIdent("2"), personopplysningGrunnlag),
                    Person(null, PersonType.BARN, null, PersonIdent("3"), personopplysningGrunnlag)
            )
        }

        every {
            integrasjonTjeneste.hentPersoninfoFor("1")
        } returns Personinfo(LocalDate.now(), null, null)

        every {
            integrasjonTjeneste.hentPersoninfoFor("2")
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE6.kode)

        every {
            integrasjonTjeneste.hentPersoninfoFor("3")
        } returns Personinfo(LocalDate.now(), null, Diskresjonskode.KODE7.kode)

        assertEquals(Diskresjonskode.KODE6.kode, finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søker, behandling))
    }

    @Test
    fun `hvis barna ikke har kode 6 eller 7 skal diskresjonskoden til søker bevares`() {
        val søkerMedAnnenKode = Personinfo(LocalDate.now(), null, "FOO")

        every {
            integrasjonTjeneste.hentPersoninfoFor(identBarn)
        } returns Personinfo(LocalDate.now(), null, "BAR")

        assertEquals("FOO", finnStrengesteDiskresjonskode.invoke(arbeidsfordelingService, søkerMedAnnenKode, behandling))
    }
}