# Origin Antifraud SDK Module

The Origin Antifraud header service implements a state-of-the-art algorithm to detect fraudulent
device activity. The Origin Antifraud SDK module is responsible for collecting, from the executing
device, signals and transmitting them to the Origin Antifraud header service for validation.

## Overview

The Antifraud SDK module collects signals from the executing device and presents them to the header
service. Signals are transmitted either in plaintext or ciphertext, depending on signal sensitivity.
For example, signals such as operating system version and hardware manufacturer are sent in
cleartext, while signals collected from user interaction or telephony services is sent in
ciphertext.

See the `AntifraudModule.java` class for which signals are sent in plaintext versus ciphertext.

## Ciphertext Signals

Signals which may leak user identity or may be used for fingerprinting are always sent in
ciphertext. The ChaCha20 symmetric stream cipher is used for ciphering these signals. The key used
during ciphering is generated once per anti-fraud validation session, and remains local to the
device. In other words, the key is never sent to the remote header service.

The remote header service utilizes hybrid symmetric transciphering, over ChaCha20, and secure
multi-party computation (MPC) to compare the ChaCha20 ciphertext signals between a device and
known fraudulent devices. To achieve this, the remote header service utilizes full homomorphic
encryption.

The ChaCha20 ciphertext signals are fed into a homomorphic circuit where the FHE encapsulated
ChaCha20 keys are used to run 20 rounds of additions, rotations, and XORs, inside the homomorphic
space.

This process strips away the ChaCha symmetric armor, without exposing the underlying plaintext
signal. Since the remote header services do not retain the FHE secret keys, only the public
keys, evaluation keys are used to determine whether signals match. These circuits can then be used
statistically or in machine learning models, without sacrificing the user's privacy.
