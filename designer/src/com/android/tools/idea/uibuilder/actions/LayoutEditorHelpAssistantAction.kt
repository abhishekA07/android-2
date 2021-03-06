/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.actions

import com.android.SdkConstants
import com.android.tools.idea.assistant.AssistantBundleCreator
import com.android.tools.idea.assistant.OpenAssistSidePanelAction
import com.android.tools.idea.assistant.datamodel.TutorialBundleData
import com.android.tools.idea.flags.StudioFlags
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.idea.debugger.readAction
import java.net.URL

/**
 * Pairs plugin bundle id to the tutorial bundle xml
 */
data class HelpPanelBundle(val bundleId: String, val bundleXml: String)

/**
 * Action that :
 * 1) Determines if the "?" icon should be displayed or not
 * 2) Performs opening Assistant Panel based on the file type that's opened.
 */
open class LayoutEditorHelpAssistantAction : OpenAssistSidePanelAction() {

  companion object {
    const val BUNDLE_ID = "LayoutEditor.HelpAssistant"
  }

  private enum class Type {
    NONE,
    CONSTRAINT_LAYOUT,
    MOTION_LAYOUT,
    FULL
  }

  private val ONLY_FULL: Boolean = true // for now redirect to a single help content
  private var tagName: String = ""
  private var type = Type.NONE

  override fun update(e: AnActionEvent) {
    updateInternalVariables(e)
    e.presentation.isEnabledAndVisible = type != Type.NONE
  }

  override fun actionPerformed(event: AnActionEvent) {
    when (type) {
      Type.CONSTRAINT_LAYOUT -> openWindow(constraintLayoutHelpPanelBundle.bundleId, event.project!!)
      Type.MOTION_LAYOUT -> openWindow(motionLayoutHelpPanelBundle.bundleId, event.project!!)
      Type.FULL -> openWindow(fullHelpPanelBundle.bundleId, event.project!!)
      Type.NONE -> Unit
    }

    // TODO: Tracker - add the event kind for assistant usage.
  }

  private fun updateInternalVariables(e: AnActionEvent) {
    tagName = tagName(e)
    if (ONLY_FULL) {
      type = Type.FULL
    } else {
      type = getType(tagName, e)
    }
  }

  private fun tagName(e: AnActionEvent): String {
    // Copy the string here rather than holding onto a field within XmlTag so it can be freely released.
    return "" + getTag(e)?.name
  }

  private fun getType(tagName: String, e: AnActionEvent): Type {
    if (tagName.isEmpty()) {
      return Type.NONE
    }

    return getDirectType(tagName).let{ type ->
      if (type != Type.NONE) {
        return@let type
      }
      /**
       * For Data Binding. It has the format like:
       * <layout>
       *   <data>  </data>
       *   <androidx...ConstraintLayout>
       */
      if (SdkConstants.TAG_LAYOUT == tagName) {
          val tag = getTag(e) ?: return@let Type.NONE
          return@let getChildrenType(tag)
        }
      return@let type
    }
  }

  private fun getTag(e: AnActionEvent): XmlTag? {
    if (!ApplicationManager.getApplication().isReadAccessAllowed) {
      return ApplicationManager.getApplication().readAction { getTag(e) }
    }
    val psiFile = e.getData(PlatformDataKeys.PSI_FILE)
    if (psiFile !is XmlFile) {
      return null
    }

    return psiFile.rootTag
  }

  private fun getChildrenType(tag: XmlTag): Type {
    if (!ApplicationManager.getApplication().isReadAccessAllowed) {
      return ApplicationManager.getApplication().readAction { getChildrenType(tag) }
    }

    tag.children.forEach {
      if (it !is XmlTag) {
        return@forEach
      }
      val childType = getDirectType(it.name)
      if (childType != Type.NONE) {
        return childType
      }
    }

    return Type.NONE
  }

  private fun getDirectType(tagName: String): Type {
    if (SdkConstants.MOTION_LAYOUT.isEquals(tagName) &&
        StudioFlags.NELE_MOTION_LAYOUT_EDITOR.get() &&
        StudioFlags.NELE_MOTION_LAYOUT_ASSISTANT.get()) {
      return Type.MOTION_LAYOUT
    } else if (SdkConstants.CONSTRAINT_LAYOUT.isEquals(tagName) &&
               StudioFlags.NELE_CONSTRAINT_LAYOUT_ASSISTANT.get()) {
      return Type.CONSTRAINT_LAYOUT
    }

    return Type.NONE
  }
}

private val motionLayoutHelpPanelBundle =
  HelpPanelBundle("LayoutEditor.HelpAssistant.MotionLayout", "/motionlayout_help_assistance_bundle.xml")

private val constraintLayoutHelpPanelBundle =
  HelpPanelBundle("LayoutEditor.HelpAssistant.ConstraintLayout", "/constraintlayout_help_assistance_bundle.xml")

private val fullHelpPanelBundle =
  HelpPanelBundle("LayoutEditor.HelpAssistant.Full", "/layout_editor_help_assistance_bundle.xml")

class MotionLayoutPanelAssistantBundleCreator :
  LayoutEditorHelpPanelAssistantBundleCreatorBase(motionLayoutHelpPanelBundle)

class ConstraintLayoutPanelAssistantBundleCreator :
  LayoutEditorHelpPanelAssistantBundleCreatorBase(constraintLayoutHelpPanelBundle)

class LayoutEditorPanelAssistantBundleCreator :
  LayoutEditorHelpPanelAssistantBundleCreatorBase(fullHelpPanelBundle)

/**
 * Base tutorial bundle xml creator.
 */
open class LayoutEditorHelpPanelAssistantBundleCreatorBase(val type: HelpPanelBundle) : AssistantBundleCreator {
  override fun getBundleId(): String {
    return type.bundleId
  }

  override fun getBundle(project: Project): TutorialBundleData? {
    return null
  }

  override fun getConfig(): URL? {
    return javaClass.getResource(type.bundleXml)
  }
}
