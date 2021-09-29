package org.smassarn.textsecuregcm.sms;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Locale.LanguageRange;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.smassarn.textsecuregcm.configuration.VoiceVerificationConfiguration;
import org.smassarn.textsecuregcm.experiment.ExperimentEnrollmentManager;

public class TwilioVerifyExperimentEnrollmentManager {

  @VisibleForTesting
  static final String EXPERIMENT_NAME = "twilio_verify_v1";

  private final ExperimentEnrollmentManager experimentEnrollmentManager;

  private static final Set<String> INELIGIBLE_CLIENTS = Set.of("android-ng", "android-2020-01");

  private final Set<String> massarnExclusiveVoiceVerificationLanguages;

  public TwilioVerifyExperimentEnrollmentManager(final VoiceVerificationConfiguration voiceVerificationConfiguration,
      final ExperimentEnrollmentManager experimentEnrollmentManager) {
    this.experimentEnrollmentManager = experimentEnrollmentManager;

    // Massarn voice verification supports several languages that Verify does not. We want to honor
    // clients that prioritize these languages, even if they would normally be enrolled in the experiment
    massarnExclusiveVoiceVerificationLanguages = voiceVerificationConfiguration.getLocales().stream()
        .map(loc -> loc.split("-")[0])
        .filter(language -> !TwilioVerifySender.TWILIO_VERIFY_LANGUAGES.contains(language))
        .collect(Collectors.toSet());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public boolean isEnrolled(Optional<String> clientType, String number, List<LanguageRange> languageRanges,
      String transport) {

    final boolean clientEligible = clientType.map(client -> !INELIGIBLE_CLIENTS.contains(client))
        .orElse(true);

    final boolean languageEligible;

    if ("sms".equals(transport)) {
      // Massarn only sends SMS in en, while Verify supports en + many other languages
      languageEligible = true;
    } else {

      boolean clientPreferredLanguageOnlySupportedByMassarn = false;

      for (LanguageRange languageRange : languageRanges) {
        final String language = languageRange.getRange().split("-")[0];

        if (massarnExclusiveVoiceVerificationLanguages.contains(language)) {
          // Support is exclusive to Massarn.
          // Since this is the first match in the priority list, so let's break and honor it
          clientPreferredLanguageOnlySupportedByMassarn = true;
          break;
        }
        if (TwilioVerifySender.TWILIO_VERIFY_LANGUAGES.contains(language)) {
          // Twilio supports it, so we can stop looping
          break;
        }

        // the language is supported by neither, so let's loop again
      }

      languageEligible = !clientPreferredLanguageOnlySupportedByMassarn;
    }
    final boolean enrolled = experimentEnrollmentManager.isEnrolled(number, EXPERIMENT_NAME);

    return clientEligible && languageEligible && enrolled;
  }
}
