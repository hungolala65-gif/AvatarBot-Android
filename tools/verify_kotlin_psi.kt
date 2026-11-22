import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

/** Parse app Kotlin files without Android SDK; syntax-only, not type checking. */
fun main(args: Array<String>) {
    require(args.size == 1) { "Usage: VerifyKtPsiKt <root>" }
    val root = File(args[0])
    val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    require(files.isNotEmpty()) { "No Kotlin app sources" }
    val disposable = Disposer.newDisposable("avatarbot-psi")
    try {
        val config = CompilerConfiguration().apply { put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE) }
        val env = KotlinCoreEnvironment.createForProduction(disposable, config, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val factory = KtPsiFactory(env.project)
        val errors = files.flatMap { file ->
            val tree = factory.createFile(file.name, file.readText())
            PsiTreeUtil.collectElementsOfType(tree, PsiErrorElement::class.java).map { "${file.name}: ${it.errorDescription}" }
        }
        if (errors.isNotEmpty()) error(errors.joinToString("\n"))
        println("PASS: Kotlin PSI syntax parsed ${files.size} app source files (semantic Gradle build still required)")
    } finally { Disposer.dispose(disposable) }
}
