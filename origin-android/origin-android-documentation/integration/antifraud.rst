.. SPDX-License-Identifier: MIT OR Apache-2.0

Antifraud
=========

The antifraud module is responsible for protecting applications against invalid traffict (IVT),
as defined by the `MRC Invalid Traffic Standards`_. This includes, but is not limited to, both
device and application integrity.

Security tests are performed, by the antifraud module, on both the device and the application. The
result of these security tests is forwarded to an attestation server. The attestation server
evaluates the security test results and produces a *signed* verdict digest. This signed verdict
digest can then used by remote backend processes to 1) verify the antifraud verdict and 2) verify
the antifraud verdict signature.

.. graphviz::
   :caption: Antifraud Architecture
   :align: center

   digraph G {
       subgraph app_lane {
           rank=same
           app[label="Application", shape=box, style=rounded]
           security_tests[label="Security Tests", shape=box]
           request_resource[label="Request with\nVerdict", shape=box]
           process_response[label="Process Response", shape=box]

           edge[color=blue, dir=none]
           app -> security_tests -> request_resource -> process_response
       }

       subgraph backend_lane {
           rank=same
           backend[label="Backend", shape=box, style=rounded]
           validate_request[label="Validate Request\nVerdict", shape=box]
           drop_request[label="Drop Request", shape=box]
           process_request[label="Process Request", shape=box]

           edge[color=purple, dir=none]
           backend -> validate_request -> drop_request -> process_request
       }

       subgraph attestation_lane {
           rank=same
           attestation[label="Attestation", shape=box, style=rounded]
           evaluate_security[label="Evaluate Security", shape=box]
           validate_verdict[label="Validate Verdict", shape=box]

           edge[color=red, dir=none]
           attestation -> evaluate_security -> validate_verdict
       }

       security_tests -> evaluate_security -> request_resource -> validate_request
       validate_request -> validate_verdict
       validate_verdict -> drop_request [label="Failed"]
       validate_verdict -> process_request[label="Successful"]
       process_request -> process_response
   }

With this attestation based architecture, the remote backend processes, which may be serving ads,
performing monitoring, or performing some expensive operation, can validate any request it receives
from the device are not fraudulent. If a request is fraudulent, then it may skip any expensive
processing, or monetary processing.

Accessing Security Verdict
--------------------------

Through the SDK, the antifraud module can be used to retrieve the signed security verdict, along
with fraud rating and confidence. The signed verdict may be used by application backend servers to
ensure a device is valid before processing its request, or restrict services to the device. The
fraud rating and confidence can be used on the device, by application code, to determine whether
functionality should be locally restricted.

As these verdicts are expensive to compute on attestation servers, they are provided lazily through
the SDK's event bus.

.. code-block:: java
   :emphasize-lines: 7-9,22-31,43-50

   package com.mycompany.myapp;

   import android.app.Application;
   import android.util.Log;

   import <vendor_namespace>.Origin;
   import <vendor_namespace>.antifraud.AntifraudModule;
   import <vendor_namespace>.antifraud.AntifraudStatus;
   import <vendor_namespace>.core.OriginModuleEventCallback;

   pubilc class MyApp extends Application {
       private static final String TAG = MyApp.class.getSimpleName();

       private OriginModuleEventCallback antifraudStatusListener;

       public MyApp() {
       }

       private void onAntifraudStatus(AntifraudStatus status) {
           Log.i(TAG, "security-verdict-digest=" + status.digest());

           if (status.isFraudulent() && status.confidence() >= 70) {
               // we're 70% confident the device or app is fraudulent
               Log.e(TAG, "device or app is fraudulent");
               // we don't have to unregister here if we don't want to
               Origin.antifraud()
                   .unregisterEventCallback(
                       this.antifraudStatusListener,
                       AntifraudModule.STATUS_UPDATE_EVENT
                   );
           }
       }

       @Override
       public void onCreate() {
           super.onCreate();

           Origin.initialize(this, Origin.CAPABILITY_ANTIFRAUD);

           this.antifraudStatusListener =
               (module, name, data, timestamp) ->
                   this.onAntifraudStatus((AntifraudStatus) data);
           // The status update event isn't sticky, get the initial
           // result, if any, right now
           this.onAntifraudStatus(Origin.antifraud().status());
           Origin.antifraud()
               .registerEventCallback(
                   this.antifraudStatusListener,
                   AntifraudModule.STATUS_UPDATE_EVENT
               );
       }
   }

.. _MRC Invalid Traffic Standards: https://mediaratingcouncil.org/sites/default/files/Standards/IVT%20Addendum%20Update%20062520.pdf
