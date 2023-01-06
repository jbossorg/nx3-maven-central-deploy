package org.jboss.nexus.validation.checks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.jboss.nexus.testutils.Utils.*;

@RunWith(MockitoJUnitRunner.class)
public class ChecksumsPresentValidationCheckTest {

        private List<FailedCheck> failedChecks;

        private ChecksumsPresentValidationCheck testObject  = new ChecksumsPresentValidationCheck();

        @Before
        public void setup() {
            failedChecks = new ArrayList<>();
        }


        @Test
        public void validateComponentEmpty() {
             Component testComponent = mock(Component.class);
             testObject.validateComponent(testComponent, Collections.emptyList(), failedChecks);
             assertTrue(failedChecks.isEmpty());
        }

        @Test
        public void validateComponentValidConfiguration() {

                //     public void validateComponent(@NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures)
                Component component1 = mock(Component.class),
                    component2 = mock(Component.class);

                Asset component1Asset = mockedAsset("something/file.jar"),
                    component1AssetCheckSumMd5 = mockedAsset("something/file.jar.md5"),
                    component1AssetCheckSumSha1 = mockedAsset("something/file.jar.sha1");

                Asset component2Asset = mockedAsset("something/file2.jar"),
                    component2AssetCheckSumMd5 = mockedAsset("something/file2.jar.md5"),
                    component2AssetCheckSumSha1 = mockedAsset("something/file2.jar.sha1");

                List<Asset> assets = new ArrayList<>();
                assets.add(component1Asset);
                assets.add(component1AssetCheckSumMd5);
                assets.add(component1AssetCheckSumSha1);

                testObject.validateComponent(component1, assets, failedChecks);
                assertTrue("Simple test should have been OK", failedChecks.isEmpty());

                assets = new ArrayList<>();
                assets.add(component2Asset);
                assets.add(component2AssetCheckSumMd5);
                assets.add(component2AssetCheckSumSha1);

                testObject.validateComponent(component1, assets, failedChecks);
                assertTrue("It should still be OK", failedChecks.isEmpty());
        }
        @Test
        public void validateComponentValidConfigurationMissingChecksums() {

                //     public void validateComponent(@NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures)
                Component component1 = mock(Component.class),
                    component2 = mock(Component.class);

                Asset component1Asset = mockedAsset("something/file.jar"),
                    component1AssetCheckSumMd5 = mockedAsset("something/file.jar.md5");
                    //removed sha1


                Asset component2Asset = mockedAsset("something/file2.jar"),
                    component2AssetCheckSumSha1 = mockedAsset("something/file2.jar.sha1"); // removed md5

                List<Asset> assets = new ArrayList<>();
                assets.add(component1Asset);
                assets.add(component1AssetCheckSumMd5);

                testObject.validateComponent(component1, assets, failedChecks);
                assertEquals("First error", 1, failedChecks.size()  );
                assertEquals("Missing checksum reported", "SHA1 checksum not found for something/file.jar", failedChecks.get(0).getProblem());
                assertSame(component1, failedChecks.get(0).getComponent());

                assets = new ArrayList<>();
                assets.add(component2Asset);
                assets.add(component2AssetCheckSumSha1);

                testObject.validateComponent(component2, assets, failedChecks);
                assertEquals("Missing checksum still there", "SHA1 checksum not found for something/file.jar", failedChecks.get(0).getProblem());
                assertEquals("Missing new checksum.", "MD5 checksum not found for something/file2.jar", failedChecks.get(1).getProblem());
                assertEquals("First error", 2, failedChecks.size() );
                assertSame(component1, failedChecks.get(0).getComponent());
                assertSame(component2, failedChecks.get(1).getComponent());
        }
}