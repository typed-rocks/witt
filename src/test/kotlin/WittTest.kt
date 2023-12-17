import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import typed.rocks.witt.WittInlayHintsProvider
import typed.rocks.witt.isTest
import java.io.File

class WittTest : MyInlayHintsProviderTestCase() {

    override fun setUp() {
        isTest = true
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(this.projectDescriptor, this.getTestName(false))
        this.myFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createCodeInsightFixture(fixtureBuilder.fixture, object : TempDirTestFixtureImpl() {
                override fun getTempDirPath(): String {
                    return "src/test/resources"
                }
            })
        myFixture.testDataPath = this.testDataPath
        myFixture.setUp()
    }

    fun `test the file`() {
        TypeScriptLanguageServiceUtil.setUseService(true)
        runHintTest("main.ts")
    }

    private fun runHintTest(testFile: String) {
        val testDataFolder = File(testDataPath)
        assertTrue(testDataFolder.exists())
        val files = testDataFolder.listFiles()
        val names = files.map { it.name }.toTypedArray()

        myFixture.configureByFiles(*names)

        val file = File("src/test/resources/testproject/$testFile")
        val expectedText = file.readText()

        doTestProvider(testFile, expectedText, WittInlayHintsProvider())
    }

    override fun getTestDataPath() = "src/test/resources/testproject"
}
