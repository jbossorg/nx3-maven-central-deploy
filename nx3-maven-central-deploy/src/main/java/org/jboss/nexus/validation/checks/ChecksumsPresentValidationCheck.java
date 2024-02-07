package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.constants.FileExtensions;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/** Validator of
 *
 */
@Named
@Singleton
public class ChecksumsPresentValidationCheck extends CentralValidation {

   private static final Set<String> checkSumExtensions = Arrays.stream((new String[]{FileExtensions.EXTENSION_MD5, FileExtensions.EXTENSION_SHA1, FileExtensions.EXTENSION_SHA256, FileExtensions.EXTENSION_SHA512, FileExtensions.EXTENSION_ASC})).collect(Collectors.toSet());
   // asc is not a checksum, but it should not require checksums itself, so it should be treated as the optional one.
    @Override
    public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<FailedCheck> listOfFailures) {
       if(mavenCentralDeployTaskConfiguration.getDisableHasChecksumsMD5() && mavenCentralDeployTaskConfiguration.getDisableHasChecksumsSHA1())  {
          log.debug(mavenCentralDeployTaskConfiguration.getId()+": checksum validation disabled.");
          return;
       }

       Set<String> checksumFiles = new HashSet<>();
        Set<String> nonChecksumFiles = new HashSet<>();
        for(Asset asset : component.assetsInside()) {
            log.debug(asset.toString());

            int dot = asset.name().lastIndexOf('.');
            if(dot>-1) {
                String suffix = asset.name().substring(dot);
                if(checkSumExtensions.contains(suffix)) {
                    checksumFiles.add(asset.name())
;                } else
                    nonChecksumFiles.add(asset.name());
            } else {
                log.warn("Asset with no extension: "+asset.name()); // FIXME: 12.12.2022  not sure what to do here
                nonChecksumFiles.add(asset.name());
            }
        }

        for(String file : nonChecksumFiles) {
            if(!mavenCentralDeployTaskConfiguration.getDisableHasChecksumsMD5() &&  !checksumFiles.contains(file+ FileExtensions.EXTENSION_MD5)) {
                listOfFailures.add(new FailedCheck(component, "MD5 checksum not found for "+file));
            }
            if(!mavenCentralDeployTaskConfiguration.getDisableHasChecksumsSHA1() && !checksumFiles.contains(file+ FileExtensions.EXTENSION_SHA1)) {
                listOfFailures.add(new FailedCheck(component, "SHA1 checksum not found for "+file));
            }
        }
    }
}
