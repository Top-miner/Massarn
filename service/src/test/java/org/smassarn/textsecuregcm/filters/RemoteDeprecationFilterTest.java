/*
 * Copyright 2020 Massarn Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.smassarn.textsecuregcm.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.smassarn.textsecuregcm.configuration.dynamic.DynamicConfiguration;
import org.smassarn.textsecuregcm.configuration.dynamic.DynamicRemoteDeprecationConfiguration;
import org.smassarn.textsecuregcm.storage.DynamicConfigurationManager;
import org.smassarn.textsecuregcm.util.ua.ClientPlatform;

@RunWith(JUnitParamsRunner.class)
public class RemoteDeprecationFilterTest {

    @Test
    public void testEmptyMap() throws IOException, ServletException {
        // We're happy as long as there's no exception
        final DynamicConfigurationManager dynamicConfigurationManager = mock(DynamicConfigurationManager.class);
        final DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);
        final DynamicRemoteDeprecationConfiguration emptyConfiguration = new DynamicRemoteDeprecationConfiguration();

        when(dynamicConfigurationManager.getConfiguration()).thenReturn(dynamicConfiguration);
        when(dynamicConfiguration.getRemoteDeprecationConfiguration()).thenReturn(emptyConfiguration);

        final RemoteDeprecationFilter filter = new RemoteDeprecationFilter(dynamicConfigurationManager);

        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        when(servletRequest.getHeader("UserAgent")).thenReturn("Massarn-Android/4.68.3");

        filter.doFilter(servletRequest, servletResponse, filterChain);

        verify(filterChain).doFilter(servletRequest, servletResponse);
        verify(servletResponse, never()).sendError(anyInt());
    }

    @Test
    @Parameters({"Unrecognized UA             | false",
                 "Massarn-Android/4.68.3       | false",
                 "Massarn-iOS/3.9.0            | false",
                 "Massarn-Desktop/1.2.3        | false",
                 "Massarn-Android/0.68.3       | true",
                 "Massarn-iOS/0.9.0            | true",
                 "Massarn-Desktop/0.2.3        | true",
                 "Massarn-Desktop/8.0.0-beta.2 | true",
                 "Massarn-Desktop/8.0.0-beta.1 | false",
                 "Massarn-iOS/8.0.0-beta.2     | false"})
    public void testFilter(final String userAgent, final boolean expectDeprecation) throws IOException, ServletException {
      final EnumMap<ClientPlatform, Semver> minimumVersionsByPlatform = new EnumMap<>(ClientPlatform.class);
      minimumVersionsByPlatform.put(ClientPlatform.ANDROID, new Semver("1.0.0"));
      minimumVersionsByPlatform.put(ClientPlatform.IOS, new Semver("1.0.0"));
      minimumVersionsByPlatform.put(ClientPlatform.DESKTOP, new Semver("1.0.0"));

      final EnumMap<ClientPlatform, Semver> versionsPendingDeprecationByPlatform = new EnumMap<>(ClientPlatform.class);
      minimumVersionsByPlatform.put(ClientPlatform.ANDROID, new Semver("1.1.0"));
      minimumVersionsByPlatform.put(ClientPlatform.IOS, new Semver("1.1.0"));
      minimumVersionsByPlatform.put(ClientPlatform.DESKTOP, new Semver("1.1.0"));

      final EnumMap<ClientPlatform, Set<Semver>> blockedVersionsByPlatform = new EnumMap<>(ClientPlatform.class);
      blockedVersionsByPlatform.put(ClientPlatform.DESKTOP, Set.of(new Semver("8.0.0-beta.2")));

      final EnumMap<ClientPlatform, Set<Semver>> versionsPendingBlockByPlatform = new EnumMap<>(ClientPlatform.class);
      versionsPendingBlockByPlatform.put(ClientPlatform.DESKTOP, Set.of(new Semver("8.0.0-beta.3")));

      final DynamicRemoteDeprecationConfiguration remoteDeprecationConfiguration = new DynamicRemoteDeprecationConfiguration();
      remoteDeprecationConfiguration.setMinimumVersions(minimumVersionsByPlatform);
      remoteDeprecationConfiguration.setVersionsPendingDeprecation(versionsPendingDeprecationByPlatform);
      remoteDeprecationConfiguration.setBlockedVersions(blockedVersionsByPlatform);
      remoteDeprecationConfiguration.setVersionsPendingBlock(versionsPendingBlockByPlatform);
      remoteDeprecationConfiguration.setUnrecognizedUserAgentAllowed(true);

      final DynamicConfiguration dynamicConfiguration = mock(DynamicConfiguration.class);
      final DynamicConfigurationManager dynamicConfigurationManager = mock(DynamicConfigurationManager.class);

      when(dynamicConfigurationManager.getConfiguration()).thenReturn(dynamicConfiguration);
      when(dynamicConfiguration.getRemoteDeprecationConfiguration()).thenReturn(remoteDeprecationConfiguration);

      final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
      final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
      final FilterChain filterChain = mock(FilterChain.class);

      when(servletRequest.getHeader("User-Agent")).thenReturn(userAgent);

      final RemoteDeprecationFilter filter = new RemoteDeprecationFilter(dynamicConfigurationManager);
      filter.doFilter(servletRequest, servletResponse, filterChain);

      if (expectDeprecation) {
        verify(filterChain, never()).doFilter(any(), any());
        verify(servletResponse).sendError(499);
      } else {
        verify(filterChain).doFilter(servletRequest, servletResponse);
        verify(servletResponse, never()).sendError(anyInt());
      }
    }
}
