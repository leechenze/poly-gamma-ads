.. SPDX-License-Identifier: MIT OR Apache-2.0

Ads
===

The ads module is responsible for maintaining active list of placements, requesting ads for
placements, scheduling rendering of ads within placements, and managing events associated with
placements. The ads module itself does not render ads, instead the module provides two types of
placement renderers, which are used to drive the advertising flow within an application.

+-----------------------------------+---------------------------+
| Placement Renderer                | Supported Ad Formats      |
+===================================+===========================+
| :code:`DisplayPlacementView`      | - Banner                  |
|                                   |   - Fixed size            |
|                                   |   - Flexible size         |
|                                   |   - Resizable             |
|                                   | - Interstitial            |
|                                   | - Native                  |
|                                   |   - Fixed size            |
|                                   |   - Flexible size         |
|                                   |   - Resizable             |
|                                   | - Rewarded                |
|                                   |   - Opt-in                |
|                                   |   - Interstitial          |
|                                   | - Splash                  |
|                                   | - Outstream               |
|                                   |   - Audio                 |
|                                   |   - Video                 |
+-----------------------------------+---------------------------+
| :code:`PlaybackPlacementRenderer` | - Instream audio          |
|                                   | - Instream video          |
|                                   | - Rewarded instream audio |
|                                   | - Rewarded instream video |
+-----------------------------------+---------------------------+

.. hint::

   Ad formats can be broadly categorized into instream and outstream. Instream ad formats are
   rendered by placements which are *interlaced* in between playback content. Outstream ad formats,
   however, are rendered by placements which are *adjacent* to any content. For example, if a user
   is consuming video content, such as a movie, any placements *interlaced* within the video content
   would be rendering some instream ad format. Consider, on the other hand, a placement which exists
   alongside some textual or playback content, such as a news website which a user is consuming the
   contents of, in this case the placement displays ad media alongside content the user is consuming,
   in which case the respective placement will be rendering outstream ad formats.

   With this, it should be clear then that :code:`DisplayPlacementView` and
   :code:`PlaybackPlacementRenderer` support outstream and instream ad formats, respectively.

.. toctree::
   :maxdepth: 1
   :titlesonly:

   placement
   scheduling
   events
   display
