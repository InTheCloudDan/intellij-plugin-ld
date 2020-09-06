package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.launchdarkly.api.model.FeatureFlag

class LDDocumentationProvider() : AbstractDocumentationProvider() {

    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): List<String>? {
        if (element == null) {
            return null
        }
        val getFlags = element.project.service<FlagStore>()
        val flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.drop(1).dropLast(1) }

        if (flag != null) {
            return listOf(flag!!.links!!.self.href)
        }

        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) {
            return null
        }
        val getFlags = element.project.service<FlagStore>()
        val flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.drop(1).dropLast(1) }
        if (flag != null) {
            val env: FlagConfiguration = getFlags.flagConfigs.get(element.text.drop(1).dropLast(1))!!
            val result = StringBuilder()
            val prereqs = if (env.prerequisites.size > 0) {
                "• <b>Prerequisites</b> ${env.prerequisites.size} • "
            } else "• "
            val rules = if (env.rules.size > 0) {
                "<b>Rules</b> ${env.rules.size} •<br />"
            } else " •"
            var targets = ""
            if (env.targets.size > 0) {
                targets += "<b>Targets:</b> "
                env.targets.forEachIndexed { i, t ->
                    targets += "${flag.variations[t.variation as Int].name ?: flag.variations[t.variation as Int].value} ${t.values.size} "
                    if (i != env.targets.lastIndex) {
                        targets += "\u2022 "
                    }
                }
                targets += "<br />"
            } else ""
            var buildEnvString = ""
            if (prereqs.length > 1) {
                buildEnvString += prereqs + " "
            }
            if (rules.length > 1) {
//                if (prereqs != "") {
//                    buildEnvString += "\u25C6 "
//                }
                buildEnvString += rules
            }
            if (targets != "") {
                buildEnvString += targets
            }
            result.append("<html>")
            result.append("<img src=\"${LDIcons.FLAG}\"> <b>LaunchDarkly Feature Flag \u2022 ${flag.name ?: flag.key}</b> <img align=\"right\" src=\"${LDIcons.FLAG}\"> <br />")
            //result.append("Enabled: ${flag.environments[settings.environment]!!.isOn}")
            val enabledIcon = if (env.on) {
                "<img src=\"${LDIcons.TOGGLE_ON}\" alt=\"On\">"
            } else {
                "<img src=\"${LDIcons.TOGGLE_OFF}\" alt=\"Off\">"
            }
            result.append("$enabledIcon • ${flag.description}<br />")
            //result.append("<pre>")
            result.append(buildEnvString)
            result.append("<br /><b>Variations</b><br />")
            flag.variations.mapIndexed { i, it ->
                var variationOut = "$i"
                if (it.name != "" && it.name != null) {
                    variationOut += " ◆ ${it.name}"
                }
                variationOut += " ◆ <code>Return value:</code> <code>${it.value}</code>"
                result.append(variationOut)
                if (it.description != "" && it.description != null) {
                    result.append("<p>${it.description ?: ""}</p><br />")
                } else {
                    result.append("<p><br /></p>")
                }
                //result.append("<p>${it.value}</p>")
            }
            //result.append("</pre>")
            result.append("</html>")

            return result.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {

        return null
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?): PsiElement? {

        return null
    }

}