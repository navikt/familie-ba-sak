package no.nav.familie.ba.sak.testfiler

object Testfil {
    val TEST_PDF = this::class.java.getResource("/dokument/mockvedtak.pdf")!!.readBytes()
    val SANITY_BEGRUNNELSER = this::class.java.getResource("/cucumber/gyldigeBegrunnelser/restSanityBegrunnelser")!!
    val SANITY_EØS_BEGRUNNELSER = this::class.java.getResource("/cucumber/gyldigeBegrunnelser/restSanityEØSBegrunnelser")!!
}
