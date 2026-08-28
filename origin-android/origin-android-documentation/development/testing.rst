.. SPDX-License-Identifier: MIT OR Apache-2.0

Testing
=======

The Origin SDK must support Android API level `19` and above, with only base AOSP features. Each SDK
module defines thorough tests validating against different API levels. Currently the following API
levels are validated against.

* `19`
* `21`
* `22`
* `25`
* `27`
* `34`
* `35`

Running
-------

The `phonesGroupCheck` test group runs tests on API levels `27` and above *only*. For validating
against all API levels, devices must be added manually.

1. Before running tests, add the devices below.

   +---------+-----------+----------+
   | Device  | API Level | Services |
   +=========+===========+==========+
   | Nexus 5 |        19 | AOSP     |
   +---------+-----------+----------+
   | Nexus 5 |        21 | AOSP     |
   +---------+-----------+----------+
   | Nexus 5 |        22 | AOSP     |
   +---------+-----------+----------+
   | Pixel   |        25 | AOSP     |
   +---------+-----------+----------+
   | Pixel 2 |        27 | AOSP     |
   +---------+-----------+----------+
   | Pixel 2 |        34 | AOSP     |
   +---------+-----------+----------+
   | Pixel 2 |        35 | AOSP     |
   +---------+-----------+----------+

2. Execute tests across all devices as described `here <https://developer.android.com/studio/test/test-in-android-studio#run-multiple-devices-parallel>`__.

Configuring
-----------

API Level: 19-21
^^^^^^^^^^^^^^^^

* Mock location must be manually enabled in `developer options <developer_options_>`_.

.. _developer_options: https://developer.android.com/studio/debug/dev-options
