.. SPDX-License-Identifier: MIT OR Apache-2.0

Placements
----------

A placement is a unique location, within an application, through which ad media is distributed.
Placements are composed of a specification and a renderer. The specification of a placement defines
the types and constraints of ad formats that it supports. Placement renderers are components which
render ad media that adheres to the specification of a placement.

.. hint::

   Each placement within an application is identified by an identifier, unique to the |vendor_name|
   Origin Platform. While it is technically possible to share placement ids across different logical
   placements, this should be avoided. Many advertising optimizations are bound to a placement id.
   If the same placement id is used across different logical placements, optimizations for a specific
   logical placement will suffer, and overall cost-per-milli (CPM) will be lower for the placement.

   The following placement identifiers can be used during development and testing:

   +------------------------+-------------------------+
   | Id                     | Enabled Ad Formats      |
   +========================+=========================+
   | :code:`test`           | - Banner                |
   |                        |   - Fixed size          |
   |                        |   - Flexible size       |
   |                        |   - Resizable           |
   |                        | - Interstitial          |
   |                        | - Native                |
   |                        |   - Fixed size          |
   |                        |   - Flexible size       |
   |                        |   - Resizable           |
   |                        | - Rewarded              |
   |                        |   - Opt-in              |
   |                        |   - Interstitial        |
   |                        | - Splash                |
   |                        | - Outstream             |
   |                        |   - Audio               |
   |                        |   - Video               |
   +------------------------+-------------------------+
   | :code:`test-banner`    | - Fixed size            |
   |                        | - Flexible size         |
   |                        | - Resizable             |
   +------------------------+-------------------------+
   | :code:`test-rewarded`  | - Opt-in                |
   |                        | - Interstitial          |
   +------------------------+-------------------------+
   | :code:`test-outstream` | - Audio                 |
   |                        | - Video                 |
   +------------------------+-------------------------+

The ads module of the Origin SDK exports only placement renderers. Placement renderers are integrated
within an application. The combination of rendering constraints and placement constraints defined in
the |vendor_name| Origin Platform Console define the full placement constraints for the respective
placement instance on the user's device. This simplifies the rendering, integration, and placement
management pipeline, as a majority of placement constraints can be updated remotely, without
requiring an application-side update.

.. graphviz::
   :caption: Placement Flow
   :align: center

   digraph G {
       app[label="Application", shape=box, style=rounded]
       platform[label="Origin Platform", shape=box, style=rounded]

       placement[label="Placement", shape=box]
       renderer[label="Placement Renderer", shape=box]

       ad[label="Ad", shape=box]
       impression[label="Impression", shape=box]

       { rank=same; app platform }
       { rank=same; placement renderer }
       { rank=same; ad impression }

       app -> renderer [label="2) DisplayPlacementView"]
       platform -> placement [label="1) Console"]

       renderer -> placement [label="3) Request Ads"]
       placement -> ad [label="4) Auction"]
       ad -> renderer [label="5) Render Ads"]
       renderer -> impression [label="6) Sale"]
       impression -> placement [label="7) Tracking"]
   }
