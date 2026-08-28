.. SPDX-License-Identifier: MIT OR Apache-2.0

Prerequisites
=============

Before integrating, the following steps must be performed.

Android API Level
-----------------

Ensure :code:`minSdk` and :code:`targetSdk` is :code:`19` or higher, and, :code:`35` or higher,
respectively.

.. code-block:: groovy
   :emphasize-lines: 11-12

   // build.gradle

   plugins {
       alias(libs.plugins.android.application)
   }

   android {
       compileSdk = 35

       defaultConfig {
           minSdk = 19
           targetSdk = 35
       }
   }

Configuring Project
-------------------

1. Include the `Maven central repository`_ within your Gradle settings.

   .. code-block:: groovy
      :emphasize-lines: 15

      // settings.gradle

      pluginManagement {
          repositories {
              google()
              mavenCentral()
              gradlePluginPortal()
          }
      }

      dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
              google()
              mavenCentral()
          }
      }

      rootProject.name = "App"
      include(":app")

2. Add the dependency for |vendor_name| Origin SDK to your Gradle build file.

   .. code-block:: groovy
      :emphasize-lines: 4

      // build.gradle

      dependencies {
          implementation("<vendor_group>:origin:[<version_current>,<version_future>)")
      }

   .. hint::

      We use a version range as minor versions are guaranteed to be backwards compatible. Using a
      version range here ensures any new feature, bug fixes, or new platform support can be
      included automatically. If a fixed version is preferred, then the version range can be
      replaced with |version_current|.

   .. note::

      When including the Origin SDK as a dependency from a precompiled AAR, the Maven coordinates
      above may be ignored, instead the AAR flat-file can be included directly, with the following
      additional dependencies.

      .. code-block:: groovy
         :emphasize-lines: 4-10

         // build.gradle

         dependencies {
             implementation(files(
                 project.layout.projectDirectory
                     .dir("libs")
                     .file("origin-release-<version_current>.aar")
             ))
             implementation("androidx.core:core:1.12.0")
             implementation("com.google.guava:listenablefuture:1.0")
         }

3. `Synchronize Gradle changes`_ by clicking **Sync Now**.

   .. image:: ../_static/intellij-sync-gradle-project.png

4. Update the application manifest as required.

   .. code-block:: xml
      :emphasize-lines: 4-7,9-12

      <?xml version="1.0" encoding="utf-8" ?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android">
          <application>
              <meta-data
                  android:name="<vendor_name_lower>.origin.application-id"
                  android:value="ORIGIN-APPLICATION-ID"
              />
              <!-- OR -->
              <meta-data
                  android:name="<vendor_name_lower>.origin.publisher-id"
                  android:value="ORIGIN-PUBLISHER-ID"
              />
          </application>
      </manifest>

   Replace :code:`ORIGIN-APPLICATION-ID` or :code:`ORIGIN-PUBLISHER-ID` with the distribution
   channel or publisher id allocated through the |vendor_name| Origin Platform. During testing
   and development, the special id :code:`test` can be used.

   .. hint::

      Only one of :code:`application-id` or :code:`publisher-id` meta-data entry is required. When
      :code:`publisher-id` is set, the combination of :code:`publisher-id` and application bundle is
      used to identify the application. Using :code:`application-id` should be preferred wherever
      possible.

   .. tip::

      When integrating the Origin SDK for only ads, :code:`application-id` and :code:`publisher-id`
      are optional as placement ids are used to identify the application. Even so, specifying at
      least one should be preferred in order to ensure integration failures are recognized by the
      |vendor_name| Origin Platform.

Region Specific
^^^^^^^^^^^^^^^

If the application build, in which the SDK is being integrated, is targeting a specific region for
legal purposes ensure the :code:`region` meta-data entry is also specified to the respective region.

.. code-block:: xml
   :emphasize-lines: 8-11

   <?xml version="1.0" encoding="utf-8" ?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">
       <application>
           <meta-data
               android:name="<vendor_name_lower>.origin.application-id"
               android:value="test"
           />
           <meta-data
               android:name="<vendor_name_lower>.origin.region"
               android:value="cn"
           />
       </application>
   </manifest>

See the table below for supported region specializations.

+--------------------------+----------------+
| Code                     | Region         |
+==========================+================+
| :code:`cn`               | China          |
+--------------------------+----------------+
| :code:`eu`               | European Union |
+--------------------------+----------------+
| :code:`global` (default) | Any region     |
+--------------------------+----------------+
| :code:`us`               | United States  |
+--------------------------+----------------+

.. _integration_prereq_gaid:

Google Mobile Service Ad Identifier
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If the application is distributed with Google Mobile Services (GMS), then the Google Advertising
Identifier (GAID) should be used to identify an instance of the application. To query the GAID for
a device, the GAID permission needs to be declared.

1. Add the dependency for :code:`play-services-ads-identifier` to your Gradle build file.

   .. code-block:: groovy
      :emphasize-lines: 4

      // build.gradle

      dependencies {
          implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")
      }

2. Declare the permission in the application manifest.

   .. code-block:: xml
      :emphasize-lines: 3-5

      <?xml version="1.0" encoding="utf-8" ?>
      <manifest xmlns:android="http://schemas.android.com/apk/res/android">
          <uses-permission
              android:name="com.google.android.gms.permission.AD_ID"
          />

          <application>
              <meta-data
                  android:name="<vendor_name_lower>.origin.application-id"
                  android:value="test"
              />
          </application>
      </manifest>

3. When uploading the application APK to Play Console answer questions regarding GAID usage
   according to the Origin SDK capabilities used.

   +----------------+-----------+-----------------+
   | SDK Capability | Analytics | Personalization |
   +================+===========+=================+
   | Antifraud      | ☑         | ☐               |
   +----------------+-----------+-----------------+
   | Advertising    | ☐         | ☑               |
   +----------------+-----------+-----------------+
   | Monitoring     | ☑         | ☐               |
   +----------------+-----------+-----------------+

.. _integration_prereq_oaid:

Open Anonymous Device Identifier
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Android Open Anonymous Device Identifier (OAID) is a user-resettable unique identifier for Android
devices, introduced by the Mobile Security Alliance (MSA) and China Information and Communication
Research Institute. OAID is supported by the Origin SDK, and should be used when the application
is targeting the Chinese market, as OAID is the primary id sanctioned for advertising within China.

.. hint::

   These instructions assume shell commands are executed from the directory the MSA SDK and artifacts
   are unzipped in. Replace :code:`/<path>/<to>/<project>` with the path to the application project.

1. Get MSA SDK from `Mobile Security Alliance`_.

2. Copy MSA SDK AAR flat-file into a local folder.

   .. code-block:: console

      $ cp oaid_sdk-2.8.0.aar /<path>/<to>/<project>/libs/

3. Copy :code:`supplierconfig.json` into the :code:`assets` folder of the application project.

   .. code-block:: console

      $ cp supplierconfig.json /<path>/<to>/<project>/src/main/assets/

4. Copy :code:`<app-bundle>.cert.pem` into the :code:`assets` folder of the application project.

   .. code-block:: console

      $ cp <app-bundle>.cert.pem /<path>/<to>/<project>/src/main/assets/

5. Add the dependency for MSA SDK to your Gradle build file.

   .. code-block:: groovy
      :emphasize-lines: 6,12-16

      // build.gradle

      android {
          defaultConfig {
              ndk {
                  abiFilters "armeabi-v7a", "x86", "arm64-v8a", "x86_64"
              }
          }
      }

      dependencies {
          implementation(files(
              project.layout.projectDirectory
                  .dir("libs")
                  .file("oaid_sdk-2.8.0.aar")
          ))
          implementation("<vendor_group>:origin:[<version_current>,<version_future>)")
      }

6. Update Proguard release rules.

   .. code-block:: bash

      #sdk
      -keep class com.bun.miitmdid.** { *; }
      -keep interface com.bun.supplier.** { *; }
      -keep class androidx.core.**{*;}
      # asus
      -keep class com.asus.msa.SupplementaryDID.** { *; }
      -keep class com.asus.msa.sdid.** { *; }
      # freeme
      -keep class com.android.creator.** { *; }
      -keep class com.android.msasdk.** { *; }
      # huawei
      -keep class com.huawei.hms.** {*;}
      -keep interface com.huawei.hms.** {*;}
      # lenovo
      -keep class com.zui.deviceidservice.** { *; }
      -keep class com.zui.opendeviceidlibrary.** { *; }
      # meizu
      -keep class com.meizu.flyme.openidsdk.** { *; }
      # nubia
      -keep class com.bun.miitmdid.provider.nubia.NubiaIdentityImpl
      # oppo
      -keep class com.heytap.openid.** { *; }
      # samsung
      -keep class com.samsung.android.deviceidservice.** { *; }
      # vivo
      -keep class com.vivo.identifier.** { *; }
      # xiaomi
      -keep class com.bun.miitmdid.provider.xiaomi.IdentifierManager
      # zte
      -keep class com.bun.lib.** { *; }
      # coolpad
      -keep class com.coolpad.deviceidsupport.** { *; }
      # EEBBK
      #None
      # honor
      -keep class com.hihonor.** {*; }
      # zuoyebang
      -keep class com.zuoyebang.iot.oaid.OaidApi {*; }

.. _Maven central repository: https://central.sonatype.com
.. _Mobile Security Alliance: https://www.msa-alliance.cn/
.. _Synchronize Gradle changes: https://developer.android.com/build#sync-files
