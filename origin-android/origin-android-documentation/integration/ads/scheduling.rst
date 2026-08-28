.. SPDX-License-Identifier: MIT OR Apache-2.0

.. _integration_ads_scheduling:

Scheduling
----------

The ads module supports preloading and caching ads on a per-placement basis. As such, at any given
point in time, there may be multiple ads available for a placement, even across application restarts,
without making a network request. In order to facilitate selecting between multiple different
possible cached ads, the ads module maintains a schedule for when cached ads will be rendered.

Ads returned by |vendor_name| Origin Platform, for a placement, specify three necessary fields which
are used when scheduling ads: price :math:`p`, minimum exposure duration :math:`d`, and expiration
timestamp :math:`t`. The price of an ad is the price, in cost-per-milli (CPM) or CPM per second (for
playback media), the buyer is willing to pay for the ad to be rendered. The minimum exposure
duration and expiration timestamp define the minimum duration the ad must be rendered for and the
timestamp after which the buyer will not consider any ad impression billable, respectively. When
scheduling a set of ads :math:`A`, then, the algorithm is as follows:

1. For every ad :math:`A_i`, sort by :math:`\alpha = t - d`.
2. Any :math:`A_i` whose :math:`\alpha` is less or equal to :math:`0` is removed.
3. Any :math:`A_i` whose immediate preceeding or succeeding ad has an :math:`\alpha` equal to the
   :math:`\alpha` of :math:`A_i` are sorted by :math:`p`, in decending order.
4. Begin preloading :math:`A_i` when its scheduled placement renderer is not currently rendering
   any ad media *or* the media currently rendered within the placement has reached :math:`80\%` of
   its minimum exposure duration :math:`d`.

Through this, ad media is served as quickly as possible, without waiting on any delays introduced
by the network connection of the device. In addition, certain placement renderers, such as
:code:`DisplayPlacementView` permit rendering multiple different ads at the same time. Through this
the effective ad request count remains stable, while the impression fill rate should remain close
to the theoretical fill rate given demand for the respective placement.
