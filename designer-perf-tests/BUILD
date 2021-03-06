load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.designer-perf-tests",
    iml_files = ["intellij.android.designer-perf-tests.iml"],
    tags = [
        "no_mac",
        "no_test_windows",
    ],
    test_class = "com.android.tools.idea.rendering.PerfgateRenderTestSuite",
    test_data = [
        "//prebuilts/studio/jdk",
        "//prebuilts/studio/layoutlib:build.prop",
        "//prebuilts/studio/layoutlib/data:framework_res.jar",
        "//prebuilts/studio/layoutlib/data:native_libs",
        "//prebuilts/studio/layoutlib/data/fonts",
        "//prebuilts/studio/layoutlib/data/icu",
        "//prebuilts/studio/sdk:build-tools/latest",
        "//prebuilts/studio/sdk:platforms/latest",
        "//prebuilts/tools/common/kotlin-plugin:Kotlin/kotlinc/build.txt",
        "//tools/adt/idea/android:test_deps.zip",
        "//tools/adt/idea/android/testData",
        "//tools/adt/idea/designer-perf-tests/testData",
        "//tools/base/build-system:gradle-distrib",
        "//tools/base/build-system:studio_repo.zip",
        "//tools/base/templates",
        "//tools/base/third_party/kotlin:kotlin-m2repository.zip",
        "//tools/idea:platform-build-info",
        "//tools/idea/java/jdkAnnotations",
    ],
    test_resources = ["testData"],
    # do not sort: must match IML order
    test_runtime_deps = [
        "//tools/idea/platform/tasks-platform-impl:intellij.platform.tasks.impl",
        "//tools/idea/platform/credential-store:intellij.platform.credentialStore",
        "//tools/idea/.idea/libraries:delight-rhino-sandbox",
        "//tools/idea/.idea/libraries:rhino",
        "//tools/idea/.idea/libraries:netty-handler-proxy",
        "//tools/idea/.idea/libraries:javassist",
        "//tools/idea/platform/diagnostic:intellij.platform.diagnostic",
        "//tools/idea/.idea/libraries:error-prone-annotations",
        "//tools/adt/idea/adt-branding:intellij.android.adt.branding",
        "//tools/idea/plugins/IntelliLang/intellilang-jps-plugin:intellij.java.langInjection.jps",
        "//tools/idea/plugins/java-decompiler/plugin:intellij.java.decompiler",
        "//tools/idea/jvm/jvm-analysis-java-tests:intellij.jvm.analysis.java.tests",
        "//tools/idea/uast/uast-tests:intellij.platform.uast.tests",
        "//tools/idea/java/manifest:intellij.java.manifest",
        "//tools/idea/java/remote-servers/impl:intellij.java.remoteServers.impl",
        "//tools/idea:intellij.java.ui.tests",
        "//tools/idea/plugins/ui-designer:intellij.java.guiForms.designer",
        "//tools/idea/java/plugin:intellij.java.plugin",
        "//tools/idea/java/compiler/instrumentation-util-8:intellij.java.compiler.instrumentationUtil.java8",
        "//tools/idea/java/ide-resources:intellij.java.ide.resources",
        "//tools/idea/plugins/ui-designer-core:intellij.uiDesigner",
        "//prebuilts/tools/common/m2/repository/com/jetbrains/intellij/documentation/tips-intellij-idea-community/193.4:jar",
        "//tools/idea/.idea/libraries:precompiled_jshell-frontend",
        "//tools/idea/plugins/maven/error-prone-compiler:intellij.maven.errorProne.compiler",
    ],
    test_srcs = ["testSrc"],
    test_timeout = "eternal",
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/base/perf-logger:studio.perf-logger[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/base/layoutlib-api:studio.android.sdktools.layoutlib-api[module, test]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/adt/idea/android-common:intellij.android.common[module, test]",
        "//tools/adt/idea/android:intellij.android.core[module, test]",
        "//tools/idea/.idea/libraries:Guava[test]",
        "//tools/idea:intellij.platform.jps.build_and_others[module, test]",
        "//tools/adt/idea/android-kotlin:intellij.android.kotlin.extensions[module, test]",
        "//tools/adt/idea/compose-designer:intellij.android.compose-designer[module, test]",
        "//tools/adt/idea/android-test-framework:intellij.android.testFramework[module, test]",
        "//tools/idea/.idea/libraries:kotlin-plugin",
    ],
)
