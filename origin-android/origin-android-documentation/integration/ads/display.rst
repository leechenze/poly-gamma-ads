.. SPDX-License-Identifier: MIT OR Apache-2.0

Display
-------

All outstream ad formats supported by Origin SDK are renderable using :code:`DisplayPlacementView`.
By default, a :code:`DisplayPlacementView` will automatically render ad media for any format which
has been 1) enabled on the |vendor_name| Origin Platform Console for the respective placement and,
2) is supported by the device on which the SDK is executing. Certain ad formats, such as
interstitial, do require SDK configuration; however, most are enabled and used by default whenever
available.

The size of display placement views is controlled, when not configured explicitly, through the size
constraints of the view when it undergoes the `layout pass <layout_pass_>`_. When an ad is available
for a display placement view, the view will automatically attempt to resize itself, either shrinking
or expanding, based on the size constraints of the available ad.

.. note::

   When ad media is rendered by a display placement view, and the view, after a subsequent layout
   pass, cannot meet the size constraints of the rendered ad media, the ads module *will* attempt
   to replace the ad media with any other ad media available which fits within the size constraints
   of the view. This is done since ad media whose size constraints cannot be met, cannot be
   impressed. In order to reduce revenue loss, replacing ad media is preferred whenever possible,
   in such situations.

.. _layout_pass: https://developer.android.com/guide/topics/ui/how-android-draws

Lifecycle
^^^^^^^^^

Display placement views begin requesting ads whenever they are attached to a display. For so long as
a view is attached to a display, it will follow the usual cycle of loading and reloading ads, based
on the :ref:`ad rendering schedule <integration_ads_scheduling>` for the placement the view renders
ad media for.

Ads may be requested manually, using :code:`beginRequestingAds()`, however, the view will *not*
actually render any ad media until it is attached to a display. Note, however, invoking
:code:`beginRequestingAds()` before setting the supported ad media size will result in an error
being thrown. Additionally, as soon as the view is detached from its display, ad requests will stop
and must be restarted either manually using :code:`beginRequestingAds()` *or* attaching the view to
a display.

.. code-block:: java
   :caption: Manually requesting ads
   :emphasize-lines: 7,22-28

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;
   import <vendor_namespace>.ads.PlacementEvent;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofPlacementId(this, "PLACEMENT_ID");

           // we want ads that are exactly 320dp wide and at least 50dp tall
           plcmt.setSupportedAdMediaSize(AdSize.ofExactWidth(320, 50));
           plcmt.setPlacementEventListener((event) -> {
               // if an ad is available, add ourself to the view hierarchy so we can render ads
               if (event.type() == PlacementEvent.EVENT_AD_AVAILABLE && plcmt.getParent() == null)
                   this.setContentView(plcmt);
           });
       }
   }

Sizing
^^^^^^

Display placement views automatically determine their maximum supported ad media size based on the
measurement from its layout passes. Supported size constraints can, however, be set explicitly using
:code:`setSupportedAdMediaSize()`. The final supported size is still calculated based on
measurements from layout passes, however, the explicitly set supported size constraints are used as
lower and upper limits for the final measurements from layout passes.

.. code-block:: java
   :caption: Explicit width and height
   :emphasize-lines: 22

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofPlacementId(this, "PLACEMENT_ID");

           // we want ads that are exactly 320x50dp
           plcmt.setSupportedAdMediaSize(AdSize.ofExact(320, 50));
           super.setContentView(plcmt);
       }
   }

.. code-block:: java
   :caption: Explicit height and minimum width
   :emphasize-lines: 22

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofPlacementId(this, "PLACEMENT_ID");

           // we want ads that are exactly 50dp tall and at least 320dp wide
           plcmt.setSupportedAdMediaSize(AdSize.ofExactHeight(50, 320));
           super.setContentView(plcmt);
       }
   }

.. code-block:: java
   :caption: Explicit width and minimum height
   :emphasize-lines: 22

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofPlacementId(this, "PLACEMENT_ID");

           // we want ads that are exactly 320dp wide and at least 50dp tall
           plcmt.setSupportedAdMediaSize(AdSize.ofExactWidth(320, 50));
           super.setContentView(plcmt);
       }
   }

.. code-block:: java
   :caption: Flexible size
   :emphasize-lines: 22

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofPlacementId(this, "PLACEMENT_ID");

           // we want ads that are 1:3, with minimum width of 50dp.
           plcmt.setSupportedAdMediaSize(AdSize.ofFlexible(1, 3, 50, 0, 0, 0));
           super.setContentView(plcmt);
       }
   }

Modal
^^^^^

Modality may be enabled for display placement views to enable support for interstitial, full screen
expandable, rewarded, and splash ad formats.

Full Screen Expandable
""""""""""""""""""""""

.. only:: build_html

   .. figure:: ../../_static/full-screen-expandable-display-placement.gif

.. code-block:: java
   :emphasize-lines: 20

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.ofBuilder(this, "PLACEMENT_ID")
                   .modality(DisplayPlacementView.MODALITY_EXPANDABLE)
                   .build();

           super.setContentView(plcmt);
       }
   }

Interstitial
""""""""""""

.. only:: build_html

   .. figure:: ../../_static/interstitial-display-placement.gif

.. code-block:: java
   :emphasize-lines: 23,33-34

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.AdSize;
   import <vendor_namespace>.ads.DisplayPlacementView;
   import <vendor_namespace>.ads.PlacementEvent;

   public class MainActivity extends AppCompatActivity {
       private DisplayPlacementView interstitialPlacement;

       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           this.interstitialPlacement =
               DisplayPlacementView.ofBuilder(this, "PLACEMENT_ID")
                   .modality(DisplayPlacementView.MODALITY_INTERSTITIAL)
                   .build();
           this.interstitialPlacement.setEventListener((event) -> {
               if (event.type() == PlacementEvent.EVENT_AD_AVAILABLE)
                   this.interstitialPlacement.show();
           });
       }

       @Override
       protected void onDestroy() {
           // Required:
           this.interstitialPlacement.destroy();
       }
   }

.. hint::

   If rewarded ads are enabled, in the |vendor_name| Origin Platform Console, for the interstitial
   placement, then rewarded ads *may* be shown within the interstitial placement. In such cases,
   ensure the :code:`EVENT_USER_REWARD` event is handled appropriately. To test whether a rewarded
   ad is rendered within an interstitial placement, the :code:`AdInstance::isRewarded()` method can
   be used.
