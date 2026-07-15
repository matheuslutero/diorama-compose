/*
 * Derived from androidx.compose.ui.test.DeviceConfigurationOverride.
 * Copyright 2023 The Android Open Source Project. Licensed under the Apache License, Version 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Vendored rather than depended on. Upstream ships this in androidx.compose.ui:ui-test, which
 * pulls Espresso, the test runner and Hamcrest into any build that takes it, and Google documents
 * it as a test-only API. Vendoring also allows the recomposition fix below.
 */
package dev.lutero.diorama

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Provides every CompositionLocal that has to move together when the configuration changes.
 *
 * All six are required. Overriding LocalConfiguration alone changes nothing for resources:
 * LocalResources reads LocalConfiguration purely to invalidate, then takes the actual Resources
 * from LocalContext — so stringResource would re-read and get the original locale back.
 *
 * The remember calls are not an optimisation. Configuration is a mutable Java class and therefore
 * unstable, so this composable can never skip; LocalContext is a static CompositionLocal, so a new
 * Context identity invalidates the whole subtree unconditionally. Upstream allocates a fresh
 * ContextThemeWrapper and FontFamilyResolver on every recomposition, which is invisible in a test
 * that composes once and recomposes the entire app every frame behind a slider.
 *
 * A fresh wrapper per distinct Configuration is mandatory: ContextThemeWrapper throws if
 * applyOverrideConfiguration is called twice on the same instance.
 */
@Composable
internal fun OverriddenConfiguration(
  configuration: Configuration,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val overriddenContext = remember(context, configuration) {
    ContextThemeWrapper(context, 0).apply { applyOverrideConfiguration(configuration) }
  }
  val fontFamilyResolver = remember(overriddenContext) { createFontFamilyResolver(overriddenContext) }

  CompositionLocalProvider(
    LocalContext provides overriddenContext,
    LocalConfiguration provides configuration,
    LocalLayoutDirection provides
      if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
      } else {
        LayoutDirection.Ltr
      },
    LocalDensity provides Density(
      density = configuration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT,
      fontScale = configuration.fontScale,
    ),
    LocalFontFamilyResolver provides fontFamilyResolver,
    content = content,
  )
  // TODO(locale): upstream also provides LocalProvidableLocaleList so LocaleList.current is
  //   correct. That local is @VisibleForTests and unreachable from outside androidx, so text-level
  //   locale needs its own plumbing here.
}
