# Origin Android SDK

Origin is an open-source advertising platform, developed by Poly-Gamma. The Origin Android SDK is
a pure-Java implementation for interacting with the *header* module of the Origin advertising
platform. The header module is responsible for tracking app events, anti-fraud validation, and
monetization.

## Features

- Supports Android 4.4 (API level 19) and above.

- No dependencies outside of Android and AndroidX.

- Extremely lightweight, with production binary builds around 200KB.

- Complete unit tests.

## Overview

- **origin-adcom:** [AdCOM][adcom] object and enumeration models.

- **origin-antifraud:** Origin Antifraud header module client.

- **origin-core:** Core SDK functionality.

- **origin-gppstring:** [GPP String][gpp] string coding.

- **origin-protobuf:** Lightweight [Protocol Buffers][protobuf] implementation.

- **origin-util:** Synchronization, collection, etc. utility definitions.

## License

This project is licensed under [Apache-2.0][apache-2.0] **OR** [MIT][mit] license.

[adcom]: https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md
[apache-2.0]: LICENSES/Apache-2.0
[gpp]: https://iabtechlab.com/gpp/
[mit]: LICENSES/MIT
[protobuf]: https://protobuf.dev/
