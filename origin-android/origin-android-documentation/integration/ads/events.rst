.. SPDX-License-Identifier: MIT OR Apache-2.0

Events
------

Advertising events are generated on a per-placement basis. These events can be listened for using
a specific placement renderer, or across all placements which have a renderer active. The
:code:`PlacementEvent` class is used to describe each event.

+---------------+----------------------------------------------------------------------------------+
| Type          | Description                                                                      |
+===============+==================================================================================+
| Error         | Error encountered while loading or rendering an ad.                              |
+---------------+----------------------------------------------------------------------------------+
| Ad Available  | Ad is available for the placement.                                               |
+---------------+----------------------------------------------------------------------------------+
| Ad Select     | Ad has been selected for rendering.                                              |
+---------------+----------------------------------------------------------------------------------+
| Ad Rendered   | Ad has been rendered.                                                            |
+---------------+----------------------------------------------------------------------------------+
| Ad Impression | Ad, rendered within placement, has received a potentially billable impression.   |
+---------------+----------------------------------------------------------------------------------+
| Ad Activated  | Ad, rendered within placement, has been activated (i.e. clicked).                |
+---------------+----------------------------------------------------------------------------------+
| Ad Removed    | Previously rendered ad, within placement, has been removed.                      |
+---------------+----------------------------------------------------------------------------------+
| Ad Resized    | Ad has been resized, for placement, based on user input, such as click.          |
+---------------+----------------------------------------------------------------------------------+
| User Reward   | Reward associated with placement is available for user.                          |
+---------------+----------------------------------------------------------------------------------+

.. graphviz::
   :caption: Event Flow
   :align: center

   digraph G {
       renderer[label="Placement Renderer", shape=box, style=rounded]

       error[label="Error Event", shape=box]
       ad_avail[label="Ad Available Event", shape=box]
       ad_select[label="Ad Selected Event", shape=box]
       ad_rendered[label="Ad Rendered Event", shape=box]
       ad_impression[label="Ad Impression Event", shape=box]
       ad_activated[label="Ad Activated Event", shape=box]
       ad_removed[label="Ad Removed Event", shape=box]
       ad_resized[label="Ad Resized Event", shape=box]
       user_reward[label="User Reward Event", shape=box]

       { rank=same; renderer }
       { rank=same; ad_avail ad_removed error }

       renderer -> error

       renderer -> ad_avail
       ad_avail -> ad_select -> ad_rendered
       ad_rendered -> ad_impression
       ad_rendered -> ad_activated
       ad_activated -> ad_resized
       ad_activated -> ad_removed
       ad_impression -> user_reward
       ad_impression -> ad_removed
       ad_rendered -> ad_removed
   }

.. hint::

   Any ad event type is always associated with the ad instance to which it referes to. The error
   event specifically may have an ad instance set if an error was encountered while ad was rendered
   or being selected for rendering.

.. code-block:: java
   :caption: Listening to all placement events
   :emphasize-lines: 7-10,23-107

   package com.mycompany.myapp;

   import android.app.Application;
   import android.util.Log;

   import <vendor_namespace>.Origin;
   import <vendor_namespace>.ads.AdInstance;
   import <vendor_namespace>.ads.AdsModule;
   import <vendor_namespace>.ads.PlacementEvent;
   import <vendor_namespace>.ads.PlacementRenderer;

   public class MyApp extends Application {
       private static final String TAG = MyApp.class.getSimpleName();

       public MyApp() {
       }

       @Override
       public void onCreate() {
           super.onCreate();

           Origin.initialize(this, Origin.CAPABILITY_ADS);
           Origin.ads().registerEventCallback((module, name, data, timestamp) -> {
               PlacementEvent event = (PlacementEvent) data;
               PlacementRenderer renderer = event.renderer();
               AdInstance ad = event.adInstance();

               switch (event.type()) {
               case PlacementEvent.EVENT_ERROR:
                   Log.w(TAG, String.format(
                       "placement %s encountered error%s",
                       renderer.placementId(),
                       ad == null ? "" :
                       String.format("while rendering ad %s", ad)
                   ), event.error());
                   break;
               case PlacementEvent.EVENT_AD_AVAILABLE:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "placement %s has ad %s available",
                       renderer.placementId(),
                       ad
                   ));
                   break;
               case PlacementEvent.EVENT_AD_SELECTED:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "placement %s has selected to render ad %s",
                       renderer.placementId(),
                       ad
                   ));
                   break;
               case PlacementEvent.EVENT_AD_RENDERED:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "placement %s has rendered ad %s",
                       renderer.placementId(),
                       ad
                   ));
                   break;
               case PlacementEvent.EVENT_AD_IMPRESSION:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "ad %s has received impression in placement %s, cpm=%s",
                       ad,
                       renderer.placementId(),
                       event.estimatedPricePerMilli()
                   ));
                   break;
               case PlacementEvent.EVENT_AD_ACTIVATED:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "ad %s has been activated in placement %s",
                       ad,
                       renderer.placementId()
                   ));
                   break;
               case PlacementEvent.EVENT_AD_REMOVED:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "ad %s has been removed from placement %s",
                       ad,
                       renderer.placementId()
                   ));
                   break;
               case PlacementEvent.EVENT_AD_RESIZED:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "ad %s has been resized in placement %s, interstitial=%s",
                       ad,
                       renderer.placementId(),
                       event.didAdExpandToInterstitial()
                   ));
                   break;
               case PlacementEvent.EVENT_USER_REWARD:
                   assert ad != null;
                   Log.i(TAG, String.format(
                       "user is receiving reward %s for ad %s in placement %s",
                       event.userReward(),
                       ad,
                       renderer.placementId()
                   ));
                   break;
               }
           }, AdsModule.PLACEMENT_EVENT);
       }
   }

.. code-block:: java
   :caption: Listening to single placement events
   :emphasize-lines: 7-8,23-37

   package com.mycompany.myapp;

   import android.os.Bundle;

   import androidx.appcompat.app.AppCompatActivity;

   import <vendor_namespace>.ads.DisplayPlacementView;
   import <vendor_namespace>.ads.PlacementEvent;

   public class MainActivity extends AppCompatActivity {
       public MainActivity() {
       }

       @Override
       protected void onCreate(Bundle state) {
           super.onCreate(state);

           DisplayPlacementView plcmt =
               DisplayPlacementView.of(super, "PLACEMENT_ID");

           // set initial playback ad media volume to 10% and 50% left and
           // right, respectively
           plcmt.setPlaybackAdMediaVolume(0.1, 0.5);
           // when an ad is selected, if it's a playback ad, increase the
           // volume to 100% if it's less than 5 seconds long
           plcmt.setPlacementEventListener((event) -> {
               if (event.type() != PlacementEvent.EVENT_AD_SELECTED)
                   return;

               if (event.adInstance().playbackDurationSeconds() < 5) {
                   plcmt.setPlaybackAdMediaVolume(1, 1);
               } else {
                   plcmt.setPlaybackAdMediaVolume(0.1, 0.5);
               }
           });
           super.setContentView(plcmt);
       }
   }
