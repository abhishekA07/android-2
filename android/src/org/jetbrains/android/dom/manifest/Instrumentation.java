/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.dom.manifest;

import com.android.SdkConstants;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.Required;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.Styleable;
import org.jetbrains.android.dom.converters.InstrumentationTargetPackageConverter;
import org.jetbrains.android.dom.converters.PackageClassConverter;

@Styleable("AndroidManifestInstrumentation")
public interface Instrumentation extends ManifestElementWithName {
  @Attribute("name")
  @Required
  @Convert(PackageClassConverter.class)
  @PackageClassConverter.Options(inheriting = SdkConstants.CLASS_INSTRUMENTATION, completeLibraryClasses = true)
  AndroidAttributeValue<PsiClass> getInstrumentationClass();

  @Required
  @Attribute("targetPackage")
  @Convert(InstrumentationTargetPackageConverter.class)
  AndroidAttributeValue<String> getTargetPackage();
}
