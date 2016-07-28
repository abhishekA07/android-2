/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework.fixture.avdmanager;

import com.android.SdkConstants;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.idea.avdmanager.AvdManagerConnection;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class MockAvdManagerConnection extends AvdManagerConnection {
  @Nullable private final AndroidSdkHandler mySdkHandler;

  public MockAvdManagerConnection(@NotNull AndroidSdkHandler handler) {
    super(handler);
    mySdkHandler = handler;
  }

  public static void inject() {
    setConnectionFactory(MockAvdManagerConnection::new);
  }

  @Override
  protected void addParameters(@NotNull AvdInfo info, @NotNull GeneralCommandLine commandLine) {
    super.addParameters(info, commandLine);
    commandLine.addParameters("-no-window");
  }

  @NotNull
  private File getAdbBinary() {
    assert mySdkHandler != null;
    return new File(mySdkHandler.getLocation(), FileUtil.join(SdkConstants.OS_SDK_PLATFORM_TOOLS_FOLDER, SdkConstants.FN_ADB));
  }

  @NotNull
  private File getAndroidBinary() {
    assert mySdkHandler != null;
    return new File(mySdkHandler.getLocation(), FileUtil.join(SdkConstants.FD_TOOLS, SdkConstants.androidCmdName()));
  }

  public void stopRunningAvd() {
    String command = getAdbBinary().getPath() + " emu kill";
    try {
      Process p = Runtime.getRuntime().exec(command);
      p.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteAvd(@NotNull String name) {
    String command = getAndroidBinary().getPath() + " delete avd -n " + name.replace(' ', '_');
    try {
      Process p = Runtime.getRuntime().exec(command);
      p.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
