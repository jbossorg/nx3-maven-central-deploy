package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.constants.FileExtensions;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

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

   static final Set<String> checkSumExtensions = Arrays.stream((new String[]{FileExtensions.EXTENSION_MD5, FileExtensions.EXTENSION_SHA1, FileExtensions.EXTENSION_SHA256, FileExtensions.EXTENSION_SHA512, FileExtensions.EXTENSION_ASC})).collect(Collectors.toSet());
   // asc is not a checksum, but it should not require checksums itself, so it should be treated as the optional one.
    @Override
    public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures) {
        Set<String> checksumFiles = new HashSet<>();
        Set<String> nonChecksumFiles = new HashSet<>();
        for(Asset asset : assets) {
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
            if(!checksumFiles.contains(file+ FileExtensions.EXTENSION_MD5)) {
                listOfFailures.add(new FailedCheck(component, "MD5 checksum not found for "+file));
            }
            if(!checksumFiles.contains(file+ FileExtensions.EXTENSION_SHA1)) {
                listOfFailures.add(new FailedCheck(component, "SHA1 checksum not found for "+file));
            }
        }
    }
}
