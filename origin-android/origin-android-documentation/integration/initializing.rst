.. SPDX-License-Identifier: MIT OR Apache-2.0

Initializing
============

The Origin SDK is organized into 3 modules, *antifraud*, *ads*, and *monitor* for device validation,
monetization, and monitoring, respectively. During initialization, one or more of these modules can
be loaded. If a module is not loaded during initialization, its capability is disabled. Additionally,
with compile-time optimizations, its likely that the code for the respective unused modules will be
removed.

Before any module can be used, the SDK must be initialized using :code:`Origin::initialize()`. This
method must be invoked within the main application thread. It should be invoked only once. If it is
invoked more than once with different capabilities, the union of capabilities for all invocations is
what will be available.

.. hint::

   The :code:`Origin::initialize()` method returns quickly. It primarily setups base structures
   required by the SDK. Module logic is initialized in the background.

The SDK may be initialized early during application startup, within the application entry-point, or
if the main and user-interface (UI) threads of the application are the same, within an activity of
the application.

1. Initializing within application entry-point.

   .. code-block:: java
      :emphasize-lines: 5,15-20

      package com.mycompany.myapp;

      import android.app.Application;

      import <vendor_namespace>.Origin;

      public class MyApp extends Application {
          public MyApp() {
          }

          @Override
          public void onCreate() {
              super.onCreate();

              // Initialize ads and antifraud modules.
              Origin.initialize(
                  this,
                  Origin.CAPABILITY_ADS |
                  Origin.CAPABILITY_ANTIFRAUD
              );
      }

2. Initialize within an activity.

   .. code-block:: java
      :emphasize-lines: 7,18

      package com.mycompany.myapp;

      import android.os.Bundle;

      import androidx.appcompat.app.AppCompatActivity;

      import <vendor_namespace>.Origin;

      public class MyActivity extends AppCompatActivity {
          public MyActivity() {
          }

          @Override
          protected void onCreate(Bundle state) {
              super.onCreate(state);

              super.setContentView(R.layout.my_activity);
              Origin.initialize(super.getApplication(), Origin.CAPABILITY_ADS);
          }
      }

.. hint::

   To initialize all modules implicitly, :code:`Origin::initialize()` may be invoked with only
   the application context, :code:`Origin.initialize(appContext)`.

Device Id
---------

Device id is usually preferred by each capability the SDK is initialized with. There are three
possible identifier initialization methods supported, depending on region and availability. See the
sections below for description of each method.

Google Advertising Identifier
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If the application is executing on a device with Google Mobile Services (GMS), and an advertising
identifier is sanctioned, then the Google Advertising Identifier (GAID) is used automatically.

.. warning::

   If GAID is not :ref:`configured <integration_prereq_gaid>`, GAID will *not* be available at all.

Open Anonymous Device Identifier
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If the application is executing on a device which supports Open Anonymous Device Identifier (OAID),
and an OAID is available, then the OAID is used automatically. Any application targeting the China
market will always use the OAID.

.. warning::

   If OAID is not :ref:`configured <integration_prereq_oaid>`, OAID will *not* be available at all.

Custom Identifier
^^^^^^^^^^^^^^^^^

When the application executes on a device which does not support any of the standard device
identifiers, a custom identifier may be supplied during SDK initialization using the
:code:`Origin::initializeWithOptions()` method.

.. code-block:: java
   :emphasize-lines: 6,16-26

   package com.mycompany.myapp;

   import android.app.Application;
   import android.util.Pair;

   import <vendor_namespace>.Origin;
   import <vendor_namespace>.OriginOptions;

   public class MyApp extends Application {
       public MyApp() {
       }

       @Override
       public void onCreate() {
           super.onCreate();

           Origin.initializeWithOptions(
               this,
               (new OriginOptions())
                   .addCapability(Origin.CAPABILITY_ADS)
                   .addDynamicDeviceId("DEVICE_ID_TYPE", (ctxt) -> new Pair<>(
                       "DEVICE_ID",
                       // `true` or `false` if user has requested limited or no ad tracking
                       false
                   ))
                   .addStaticDeviceId("DEVICE_ID_TYPE", "DEVICE_ID", false)
           );
       }
   }

Replace :code:`DEVICE_ID_TYPE` and :code:`DEVICE_ID` with the device identifier type and device
identifier value, respectively. The identifier type should be a unique type name of the identifier
type. For example, the identifier type of Google Advertiser Identifier is :code:`"gaid"`.

Two types of custom identifiers are supported, *dynamic* and *static*, specified using
:code:`addDynamicDeviceId` and :code:`addStaticDeviceId`, respectively. Dynamic device identifiers
may change during the lifecycle of an application execution, and are provided using a provider
function, which is invoked, on a worker thread, with the application context. Static device
identifiers, however, do not change during the lifecycle of an application execution.

.. hint::

   Multiple custom device identifiers may be specified by invoking :code:`addDynamicDeviceId` or
   :code:`addStaticDeviceId` more than once.

.. warning::

   When providing custom device identifiers, ensure the identifiers do have some form of stability.
   In other words, an identifier which changes each time the application is restarted will likely
   result in poor performance.
