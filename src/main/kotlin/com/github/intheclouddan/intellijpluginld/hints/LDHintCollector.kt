package com.github.intheclouddan.intellijpluginld.hints

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.coderefs.FlagAliases
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.ColorUtil
import com.launchdarkly.api.model.FeatureFlag
import java.awt.Color

@Suppress("UnstableApiUsage")
class LDHintCollector(editor: Editor) : InlayHintsCollector {
    private val factory = PresentationFactory(editor as EditorImpl)

    private val colorValidatedGreen: Color
    private val colorValidatedBlue: Color
    private val colorNotValidated: Color

    init {
        val baseColor =
            editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT).foregroundColor

        colorValidatedGreen = ColorUtil.mix(baseColor, Color.GREEN, 0.2)
        colorValidatedBlue = ColorUtil.mix(baseColor, Color.CYAN, 0.2)
        colorNotValidated = ColorUtil.mix(baseColor, Color.YELLOW, 0.2)
    }

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
//        val name = when(element){
//            is PsiIdentifier -> element.text
//            else -> null
//        }
//        if (settings.showHints == false) {
//            return false
//        }
        println("collecting")
        val getFlags = element.project.service<FlagStore>()
        val getAliases = element.project.service<FlagAliases>()
        //val settings = LaunchDarklyMergedSettings.getInstance(element.project)
        println(element.text)
        var flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.removeSurrounding("\"") }
        var alias: String?
        if (flag == null) {
            alias = getAliases.aliases[element.text.removeSurrounding("\"")]
            flag = getFlags.flags.items.find { it.key == alias }
        }
        // TODO: gracefully handle API call working and Datastore being unavailable
        if (flag == null) {
            return false
        }
        val entry = flag
        // val entry = name?.let(mappings::map) ?: return true
        val hint = createHint(entry)

        //if (settings.displayHintsInline){
        sink.addInlineElement(
            element.endOffset,
            relatesToPrecedingText = true,
            presentation = InsetPresentation(hint, top = 3, left = 1, right = 1),
            true
        )
        //}

        val doc = editor.document
        val offset = element.startOffset
        val column = offset - doc.getLineStartOffset(doc.getLineNumber(offset))

        val presentation = SequencePresentation(
            listOf(
                SpacePresentation(column * EditorUtil.getPlainSpaceWidth(editor), 0),
                hint
            )
        )

        sink.addBlockElement(
            offset = offset,
            showAbove = true,
            presentation = presentation,
            relatesToPrecedingText = false,
            priority = 100
        )

        return true
    }

    private fun createHint(entry: FeatureFlag): InlayPresentation {
        val hint = factory.smallText(entry.key)

        return createForegroundColorPresentation(hint, colorNotValidated)
    }

    private fun createForegroundColorPresentation(wrapped: InlayPresentation, color: Color): InlayPresentation {
        return AttributesTransformerPresentation(wrapped) {
            it.clone().apply { foregroundColor = color }
        }
    }
}
