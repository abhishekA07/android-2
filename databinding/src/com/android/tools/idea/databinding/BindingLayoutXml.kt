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
package com.android.tools.idea.databinding

import com.android.SdkConstants
import com.android.tools.idea.res.BindingLayoutData
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * This file contains extension methods that help the data binding codebase search an [XmlFile] for
 * tags related to data binding layouts.
 */

/**
 * Finds the corresponding [XmlTag] for the first `<variable>` tag whose `name` attribute matches
 * [name], or null if not found.
 */
fun XmlFile.findVariableTag(name: String): XmlTag? {
  val dataTag = findDataTag() ?: return null
  for (tag in dataTag.findSubTags("variable")) {
    val currName = tag.getAttributeValue(SdkConstants.ATTR_NAME) ?: return null
    if (name == StringUtil.unescapeXmlEntities(currName)) {
      return tag
    }
  }

  return null
}

/**
 * Finds the corresponding [XmlTag] for the first `<import>` tag whose short name (i.e. `alias`, if
 * set, or otherwise `type` after removing its qualified path) matches [shortName], or null if not
 * found.
 */
fun XmlFile.findImportTag(shortName: String): XmlTag? {
  val dataTag = findDataTag() ?: return null
  for (tag in dataTag.findSubTags("import")) {
    val alias = tag.getAttributeValue(SdkConstants.ATTR_ALIAS)
    if (alias == null) {
      val type = tag.getAttributeValue(SdkConstants.ATTR_TYPE) ?: continue
      if (shortName == BindingLayoutData.Import.getImportedShortName(StringUtil.unescapeXmlEntities(type), null)) {
        return tag
      }
    }
    else if (shortName == StringUtil.unescapeXmlEntities(alias)) {
      return tag
    }
  }

  return null
}

/**
 * Finds the first `<data>` tag contained inside a root `<layout>` tag in this file, or null if not
 * found.
 */
private fun XmlFile.findDataTag(): XmlTag? {
  val rootTag = rootTag ?: return null
  if (rootTag.name == "layout") {
    return rootTag.findFirstSubTag("data")
  }

  return null
}