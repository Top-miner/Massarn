/*
 * Copyright 2021 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.badges;

import java.util.Locale;
import java.util.ResourceBundle;

public interface ResourceBundleFactory {
  ResourceBundle createBundle(String baseName, Locale locale, ResourceBundle.Control control);
}
