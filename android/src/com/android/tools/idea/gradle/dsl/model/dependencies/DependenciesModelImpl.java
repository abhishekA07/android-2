/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.gradle.dsl.model.dependencies;

import static com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel.ValueType.NONE;
import static com.android.tools.idea.gradle.dsl.model.ext.PropertyUtil.followElement;

import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencySpec;
import com.android.tools.idea.gradle.dsl.api.dependencies.DependenciesModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.DependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.FileDependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.FileTreeDependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.ModuleDependencyModel;
import com.android.tools.idea.gradle.dsl.model.GradleDslBlockModel;
import com.android.tools.idea.gradle.dsl.parser.dependencies.DependenciesDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslClosure;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslExpression;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslExpressionList;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslExpressionMap;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslLiteral;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslMethodCall;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslSimpleExpression;
import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class DependenciesModelImpl extends GradleDslBlockModel implements DependenciesModel {
  public DependenciesModelImpl(@NotNull DependenciesDslElement dslElement) {
    super(dslElement);
  }

  /**
   * A DependencyModel collector configured to fetch a specific type of depndencies from a GradleDslElement.
   */
  private interface Collector<T extends DependencyModel> {
    void add(T element);
    void addAll(@NotNull Collection<? extends T> elements);
    List<T> getResults();

    void collect(@NotNull String configurationName,
               @NotNull GradleDslElement element,
               @Nullable GradleDslClosure configurationElement);
  }

  /**
   * A strategy object to find and get specific DependencyModel objects from a GradleDslElement.
   */
  private interface Fetcher<T extends DependencyModel> {
    default Collector<T> createCollector(@NotNull Fetcher<T> rootFetcher) {
      return new DependencyModelCollector<>(rootFetcher);
    }

    void fetch(@NotNull String configurationName,
               @NotNull GradleDslElement element,
               @NotNull GradleDslElement resolved,
               @Nullable GradleDslClosure configurationElement,
               @NotNull Collector<? super T> dest);
  }

  private static class DependencyModelCollector<T extends DependencyModel> implements Collector<T> {
    private final Fetcher<T> myRootFetcher;
    private List<T> myResult;

    public DependencyModelCollector(Fetcher<T> rootFetcher) {
      myRootFetcher = rootFetcher;
      myResult = new ArrayList<>();
    }

    @Override
    public void add(T element) {
      myResult.add(element);
    }

    @Override
    public void addAll(@NotNull Collection<? extends T> elements) {
      myResult.addAll(elements);
    }

    @Override
    public List<T> getResults() {
      return myResult;
    }

    @Override
    public void collect(@NotNull String configurationName,
                        @NotNull GradleDslElement element,
                        @Nullable GradleDslClosure configurationElement) {
      GradleDslElement resolved = element;
      if (element instanceof GradleDslLiteral) {
        GradleDslElement foundElement = followElement((GradleDslLiteral)element);
        if (foundElement instanceof GradleDslExpression) {
          resolved = (GradleDslExpression)foundElement;
        }
      }
      if (resolved instanceof GradleDslExpressionList) {
        for (GradleDslExpression expression : ((GradleDslExpressionList)resolved).getExpressions()) {
          this.collect(configurationName, expression, configurationElement);
        }
      }
      else {
        myRootFetcher.fetch(configurationName, element, resolved, configurationElement, this);
      }
    }
  }

  private final static Fetcher<ArtifactDependencyModel> ourArtifactFetcher = new Fetcher<ArtifactDependencyModel>() {
    @Override
    public void fetch(@NotNull String configurationName,
                      @NotNull GradleDslElement element,
                      @NotNull GradleDslElement resolved,
                      @Nullable GradleDslClosure configurationElement,
                      @NotNull Collector<? super ArtifactDependencyModel> dest) {
      if (configurationElement == null) {
        configurationElement = element.getClosureElement();
      }

      // We can only create ArtifactDependencyModels from expressions, if for some reason we don't have an expression here (e.g form a
      // parser bug) then don't create anything.
      if (!(element instanceof GradleDslExpression)) {
        return;
      }

      if (resolved instanceof GradleDslExpressionMap) {
        ArtifactDependencyModelImpl.MapNotation mapNotation =
          ArtifactDependencyModelImpl.MapNotation.create(configurationName, (GradleDslExpressionMap)resolved, configurationElement);
        if (mapNotation != null) {
          dest.add(mapNotation);
        }
      }
      else if (resolved instanceof GradleDslMethodCall) {
        String name = ((GradleDslMethodCall)resolved).getMethodName();
        if (!"project".equals(name) && !"fileTree".equals(name) && !"files".equals(name)) {
          for (GradleDslElement argument : ((GradleDslMethodCall)resolved).getArguments()) {
            dest.collect(configurationName, argument, configurationElement);
          }
        }
      }
      else {
        ArtifactDependencyModelImpl.CompactNotation compactNotation = ArtifactDependencyModelImpl.CompactNotation
          .create(configurationName, (GradleDslSimpleExpression)element, configurationElement);
        if (compactNotation != null) {
          dest.add(compactNotation);
        }
      }
    }
  };

  private final static Fetcher<ModuleDependencyModel> ourModuleFetcher = new Fetcher<ModuleDependencyModel>() {
    @Override
    public void fetch(@NotNull String configurationName,
                      @NotNull GradleDslElement element,
                      @NotNull GradleDslElement resolved,
                      @Nullable GradleDslClosure configurationElement,
                      @NotNull Collector<? super ModuleDependencyModel> dest) {
      if (resolved instanceof GradleDslMethodCall) {
        GradleDslMethodCall methodCall = (GradleDslMethodCall)resolved;
        if (methodCall.getMethodName().equals(ModuleDependencyModelImpl.PROJECT)) {
          ModuleDependencyModel model = ModuleDependencyModelImpl.create(configurationName, methodCall);
          if (model != null && model.path().getValueType() != NONE) {
            dest.add(model);
          }
        }
      }
    }
  };

  private final static Fetcher<FileDependencyModel> ourFileFetcher = new Fetcher<FileDependencyModel>() {
    @Override
    public void fetch(@NotNull String configurationName,
                      @NotNull GradleDslElement element,
                      @NotNull GradleDslElement resolved,
                      @Nullable GradleDslClosure configurationElement,
                      @NotNull Collector<? super FileDependencyModel> dest) {
      if (resolved instanceof GradleDslMethodCall) {
        GradleDslMethodCall methodCall = (GradleDslMethodCall)resolved;
        if (methodCall.getMethodName().equals(FileDependencyModelImpl.FILES)) {
          dest.addAll(FileDependencyModelImpl.create(configurationName, methodCall));
        }
      }
    }
  };

  private final static Fetcher<FileTreeDependencyModel> ourFileTreeFetcher = new Fetcher<FileTreeDependencyModel>() {
    @Override
    public void fetch(@NotNull String configurationName,
                      @NotNull GradleDslElement element,
                      @NotNull GradleDslElement resolved,
                      @Nullable GradleDslClosure configurationElement,
                      @NotNull Collector<? super FileTreeDependencyModel> dest) {
      if (resolved instanceof GradleDslMethodCall) {
        GradleDslMethodCall methodCall = (GradleDslMethodCall)resolved;
        if (methodCall.getMethodName().equals(FileTreeDependencyModelImpl.FILE_TREE)) {
          FileTreeDependencyModel model = FileTreeDependencyModelImpl.create(methodCall, configurationName);
          if (model != null && model.dir().getValueType() != NONE) {
            dest.add(model);
          }
        }
      }
    }
  };

  private final static Fetcher<DependencyModel> ourAllFetcher = new Fetcher<DependencyModel>() {
    @Override
    public void fetch(@NotNull String configurationName,
                      @NotNull GradleDslElement element,
                      @NotNull GradleDslElement resolved,
                      @Nullable GradleDslClosure configurationElement,
                      @NotNull Collector<? super DependencyModel> dest) {
      ourArtifactFetcher.fetch(configurationName, element, resolved, configurationElement, dest);
      ourModuleFetcher.fetch(configurationName, element, resolved, configurationElement, dest);
      ourFileFetcher.fetch(configurationName, element, resolved, configurationElement, dest);
      ourFileTreeFetcher.fetch(configurationName, element, resolved, configurationElement, dest);
    }
  };


  /**
   * @return all the dependencies (artifact, module, etc.)
   * WIP: Do not use:)
   */
  @NotNull
  @Override
  public List<DependencyModel> all() {
    return all(null, ourAllFetcher);
  }

  @NotNull
  private <T extends DependencyModel> List<T> all(@Nullable String configurationName, @NotNull Fetcher<T> fetcher) {
    Collector<T> dependencies = fetcher.createCollector(fetcher);
    for (GradleDslElement element : configurationName != null
                                    ? myDslElement.getPropertyElementsByName(configurationName)
                                    : myDslElement.getAllPropertyElements()) {
      dependencies.collect(element.getName(), element, null);
    }
    return dependencies.getResults();
  }

  @NotNull
  @Override
  public List<ArtifactDependencyModel> artifacts(@NotNull String configurationName) {
    return all(configurationName, ourArtifactFetcher);
  }

  @NotNull
  @Override
  public List<ArtifactDependencyModel> artifacts() {
    return all(null, ourArtifactFetcher);
  }

  @Override
  public boolean containsArtifact(@NotNull String configurationName, @NotNull ArtifactDependencySpec dependency) {
    for (ArtifactDependencyModel artifactDependencyModel : artifacts(configurationName)) {
      if (ArtifactDependencySpecImpl.create(artifactDependencyModel).equals(dependency)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void addArtifact(@NotNull String configurationName, @NotNull String compactNotation) {
    ArtifactDependencySpec dependency = ArtifactDependencySpecImpl.create(compactNotation);
    if (dependency == null) {
      String msg = String.format("'%1$s' is not a valid artifact dependency", compactNotation);
      throw new IllegalArgumentException(msg);
    }
    addArtifact(configurationName, dependency);
  }

  @Override
  public void addArtifact(@NotNull String configurationName, @NotNull ArtifactDependencySpec dependency) {
    addArtifact(configurationName, dependency, Collections.emptyList());
  }

  @Override
  public void addArtifact(@NotNull String configurationName,
                          @NotNull ArtifactDependencySpec dependency,
                          @NotNull List<ArtifactDependencySpec> excludes) {
    ArtifactDependencyModelImpl.createNew(myDslElement, configurationName, dependency, excludes);
  }

  @Override
  public boolean replaceArtifactByPsiElement(@NotNull PsiElement psiElement, @NotNull ArtifactDependencySpec dependency) {
    GradleDslElement element = findByPsiElement(psiElement);
    if (element == null) {
      return false;
    }

    performDependencyReplace(psiElement, element, dependency);
    return true;
  }

  @NotNull
  @Override
  public List<ModuleDependencyModel> modules() {
    return all(null, ourModuleFetcher);
  }


  @Override
  public void addModule(@NotNull String configurationName, @NotNull String path) {
    addModule(configurationName, path, null);
  }

  @Override
  public void addModule(@NotNull String configurationName, @NotNull String path, @Nullable String config) {
    ModuleDependencyModelImpl.createNew(myDslElement, configurationName, path, config);
  }

  @NotNull
  @Override
  @TestOnly
  public List<FileTreeDependencyModel> fileTrees() {
    return all(null, ourFileTreeFetcher);
  }

  @Override
  public void addFileTree(@NotNull String configurationName, @NotNull String dir) {
    addFileTree(configurationName, dir, null, null);
  }

  @Override
  public void addFileTree(@NotNull String configurationName,
                          @NotNull String dir,
                          @Nullable List<String> includes,
                          @Nullable List<String> excludes) {
    FileTreeDependencyModelImpl.createNew(myDslElement, configurationName, dir, includes, excludes);
  }

  @NotNull
  @Override
  @TestOnly
  public List<FileDependencyModel> files() {
    return all(null, ourFileFetcher);
  }

  @Override
  public void addFile(@NotNull String configurationName, @NotNull String file) {
    FileDependencyModelImpl.createNew(myDslElement, configurationName, file);
  }

  @Override
  public void remove(@NotNull DependencyModel dependency) {
    if (!(dependency instanceof DependencyModelImpl)) {
      Logger.getInstance(DependenciesModelImpl.class)
        .warn("Tried to remove an unknown dependency type!");
      return;
    }
    GradleDslElement dependencyElement = ((DependencyModelImpl)dependency).getDslElement();
    GradleDslElement parent = dependencyElement.getParent();
    if (parent instanceof GradleDslMethodCall) {
      GradleDslMethodCall methodCall = (GradleDslMethodCall)parent;
      List<GradleDslExpression> arguments = methodCall.getArguments();
      if (arguments.size() == 1 && arguments.get(0).equals(dependencyElement)) {
        // If this is the last argument, remove the method call altogether.
        myDslElement.removeProperty(methodCall);
      }
      else {
        methodCall.remove(dependencyElement);
      }
    }
    else if (parent instanceof GradleDslExpressionList) {
      List<GradleDslExpression> expressions = ((GradleDslExpressionList)parent).getExpressions();
      if (expressions.size() == 1 && expressions.get(0).equals(dependencyElement)) {
        if (parent.getParent() instanceof GradleDslMethodCall) {
          // We need to delete up two levels if this is a method call.
          myDslElement.removeProperty(parent.getParent());
        }
        else {
          myDslElement.removeProperty(parent);
        }
      }
      else {
        ((GradleDslExpressionList)parent).removeElement(dependencyElement);
      }
    }
    else {
      myDslElement.removeProperty(dependencyElement);
    }
  }

  private static void performDependencyReplace(@NotNull PsiElement psiElement,
                                               @NotNull GradleDslElement element,
                                               @NotNull ArtifactDependencySpec dependency) {
    if (element instanceof GradleDslLiteral) {
      ((GradleDslLiteral)element).setValue(dependency.compactNotation());
    }
    else if (element instanceof GradleDslExpressionMap) {
      updateGradleExpressionMapWithDependency((GradleDslExpressionMap)element, dependency);
    }
    else if (element instanceof GradleDslMethodCall) {
      // There may be multiple arguments here, check find the one with correct PsiElement.
      GradleDslMethodCall methodCall = (GradleDslMethodCall)element;
      for (GradleDslElement e : methodCall.getArguments()) {
        if (e.getPsiElement() == psiElement) {
          performDependencyReplace(psiElement, e, dependency);
        }
      }
    }
    else if (element instanceof GradleDslExpressionList) {
      for (GradleDslSimpleExpression expression : ((GradleDslExpressionList)element).getSimpleExpressions()) {
        if (element.getPsiElement() == psiElement) {
          performDependencyReplace(psiElement, expression, dependency);
        }
      }
    }
  }

  /**
   * Updates a {@link GradleDslExpressionMap} so that it represents the given {@link ArtifactDependencySpec}.
   */
  private static void updateGradleExpressionMapWithDependency(@NotNull GradleDslExpressionMap map,
                                                              @NotNull ArtifactDependencySpec dependency) {
    // We need to create a copy of the new map so that we can track the r
    Map<String, Function<ArtifactDependencySpec, String>> properties = new LinkedHashMap<>(ArtifactDependencySpecImpl.COMPONENT_MAP);
    // Update any existing properties.
    for (Map.Entry<String, GradleDslElement> entry : map.getPropertyElements().entrySet()) {
      if (properties.containsKey(entry.getKey())) {
        String value = properties.get(entry.getKey()).fun(dependency);
        if (value == null) {
          continue;
        }

        map.setNewLiteral(entry.getKey(), value);
        properties.remove(entry.getKey());
      }
      else {
        map.removeProperty(entry.getKey()); // Removes any unknown properties.
      }
    }
    // Add the remaining properties.
    for (Map.Entry<String, Function<ArtifactDependencySpec, String>> entry : properties.entrySet()) {
      String value = entry.getValue().fun(dependency);
      if (value != null) {
        map.addNewLiteral(entry.getKey(), value);
      }
      else {
        map.removeProperty(entry.getKey()); // Remove any properties that are null in the new dependency,
      }
    }
  }

  /**
   * Returns {@code true} if {@code child} is a descendant of the {@code parent}, {@code false} otherwise.
   */
  private static boolean isChildOfParent(@NotNull PsiElement child, @NotNull PsiElement parent) {
    List<PsiElement> childElements = Lists.newArrayList(parent);
    while (!childElements.isEmpty()) {
      PsiElement element = childElements.remove(0);
      if (element.equals(child)) {
        return true;
      }
      childElements.addAll(Arrays.asList(element.getChildren()));
    }
    return false;
  }

  /**
   * Finds a {@link GradleDslElement} corresponding to an artifact which is represented by the given {@link PsiElement}. This method will
   * split up
   */
  @Nullable
  private GradleDslElement findByPsiElement(@NotNull PsiElement child) {
    for (String configurationName : myDslElement.getProperties()) {
      for (GradleDslElement element : myDslElement.getPropertyElementsByName(configurationName)) {
        // For method calls we need to check each of the arguments individually.
        if (element instanceof GradleDslMethodCall) {
          GradleDslMethodCall methodCall = (GradleDslMethodCall)element;
          for (GradleDslElement el : methodCall.getArguments()) {
            if (el.getPsiElement() != null && isChildOfParent(child, el.getPsiElement())) {
              return el;
            }
          }
        }
        else if (element instanceof GradleDslExpressionList) {
          for (GradleDslSimpleExpression e : ((GradleDslExpressionList)element).getSimpleExpressions()) {
            if (e.getPsiElement() != null && isChildOfParent(child, e.getPsiElement())) {
              return e;
            }
          }
        }
        else {
          if (element.getPsiElement() != null && isChildOfParent(child, element.getPsiElement())) {
            return element;
          }
        }
      }
    }
    return null;
  }
}
