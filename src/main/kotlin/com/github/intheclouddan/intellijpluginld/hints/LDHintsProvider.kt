package com.github.intheclouddan.intellijpluginld.hints

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class LDHintsProvider : InlayHintsProvider<NoSettings> {
    override val key = SettingsKey<NoSettings>("LaunchDarklyHints")
    override val name = "LaunchDarkly"
    override val previewText: String? = null

    override fun createConfigurable(settingsLD: NoSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JPanel = panel {}

        override val cases: List<ImmediateConfigurable.Case> = emptyList()
    }

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return LDHintCollector(editor)
    }

    override fun isLanguageSupported(language: Language): Boolean {
        return true
    }
}

//data class NoSettings(
//    var showHints: Boolean = true
//)