package com.github.intheclouddan.intellijpluginld.coderefs

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.charset.Charset

@Service
class FlagAliases(private var project: Project) {
    var aliases = mutableMapOf<String, String>()
    val cr = CodeRefs()
    //lateinit var aliasMap: Map<String, List<String>>
//    val getAliases = ProjectManager.getInstance()
//    val stuff = getAliases.
    //project.service<FlagAliases>()
    //val projectService = project.service<MyProjectService>()


    fun readAliases(file: File) {
        println(file)
        //val rows: List<Map<String, String>> =
        csvReader().open(file) {
            readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                //Do something
                println(row)
                println(row["aliases"])
                if (row["aliases"] !== "") {
                    //aliases.set(row["aliases"], row["flagKey"])
                    aliases[row["aliases"]!!] = row["flagKey"]!!
                }
            }
        }
        println(aliases)
    }

    fun runCodeRefs(project: Project) {
        try {
            val settings = LaunchDarklyMergedSettings.getInstance(project)
            val tmpDir = File(PathManager.getPluginTempPath())
            Thread.sleep(25000)
            val cmds = ArrayList<String>()
            val aliasesPath = File(project.basePath + "/.launchdarkly/coderefs.yaml")
            println("Alias path ${aliasesPath.exists()}")
            if (!aliasesPath.exists()) {
                return
            }
            cmds.add(PathManager.getPluginsPath() + "/intellij-plugin-ld/bin/coderefs/2.1.0/ld-find-code-refs")
            cmds.add("--dir=${project.basePath}")
            cmds.add("--dryRun")
            cmds.add("--outDir=$tmpDir")
            cmds.add("--projKey=${settings.project}")
            cmds.add("--repoName=${project.name}")
            cmds.add("--baseUri=${settings.baseUri}")
            cmds.add("--contextLines=-1")
            cmds.add("--branch=scan")
            cmds.add("--revision=0")
            val generalCommandLine = GeneralCommandLine(cmds)
            generalCommandLine.charset = Charset.forName("UTF-8")
            generalCommandLine.setWorkDirectory(project.basePath)
            generalCommandLine.withEnvironment("LD_ACCESS_TOKEN", settings.authorization)

            try {
                val processHandler: ProcessHandler = OSProcessHandler(generalCommandLine)
                processHandler.startNotify()
                processHandler.waitFor()
            } catch (exception: ExecutionException) {
                println(exception)
            }
            val aliasPath = File(PathManager.getPluginTempPath() + "/coderefs_${settings.project}_${project.name}_scan.csv")
            readAliases(aliasPath)

        } catch (err: Exception) {
            println(err)
        }

    }

    fun checkCodeRefs(): Boolean {
        val pluginPath = File("${cr.codeRefsPath}${cr.codeRefsVerion}")
        if (!pluginPath.exists()) {
            // If this version of CodeRefs is not downloaded. Wipe out bin dir of all versions and redownload.
            val binDir = File("${cr.codeRefsPath}")
            try {
                binDir.deleteRecursively()
                ApplicationManager.getApplication().executeOnPooledThread {
                    cr.downloadCodeRefs()
                }
            } catch (err: Exception) {
                println(err)
                return false
            }
        }
        return true
    }

    init {
        val runnerCheck = checkCodeRefs()
        if (runnerCheck) {
            ApplicationManager.getApplication().executeOnPooledThread {
                runCodeRefs(project)
            }
        }
    }
}