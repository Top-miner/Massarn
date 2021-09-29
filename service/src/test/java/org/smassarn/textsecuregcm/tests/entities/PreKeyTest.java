/*
 * Copyright 2013-2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.tests.entities;

import org.junit.Test;
import org.smassarn.textsecuregcm.entities.PreKey;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.smassarn.textsecuregcm.tests.util.JsonHelpers.*;

public class PreKeyTest {

  @Test
  public void serializeToJSONV2() throws Exception {
    PreKey preKey = new PreKey(1234, "test");

    assertThat("PreKeyV2 Serialization works",
               asJson(preKey),
               is(equalTo(jsonFixture("fixtures/prekey_v2.json"))));
  }

}
