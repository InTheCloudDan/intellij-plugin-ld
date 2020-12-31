package com.github.intheclouddan.intellijpluginld.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel

@Suppress("UnstableApiUsage")
class LDHintsProvider : InlayHintsProvider<HintSettings> {
    override val key = SettingsKey<HintSettings>("LaunchDarklyHints")

    override val name = "LaunchDarkly Hint Suggestions"
    override val previewText: String? = null

    override fun createConfigurable(settings: HintSettings) = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener) = panel {
            row {
                checkBox("Show Hints", settings::showHints)
            }
        }

        override val cases
            get() = listOf(
                Case("Show missing", "show.missing", settings::showHints),
            )
    }

    override fun createSettings(): HintSettings {
        return HintSettings()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: HintSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return LDHintCollector(editor, settings)
    }

    override fun isLanguageSupported(language: Language): Boolean {
        return true
    }
}