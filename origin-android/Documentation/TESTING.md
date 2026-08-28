# Testing

Origin supports Android API level `19` and above. In order to ensure SDK modules are validated,
each release is validated against the following API levels, on base AOSP images.

* `19`
* `21`
* `22`
* `25`
* `27`
* `34`
* `35`

## Development

The `phonesGroupCheck` test group runs tests on API levels `27` and above. For validating against
older API levels, devices must be added manually. Before running tests, add the devices below.
Then execute all tests [across](https://developer.android.com/studio/test/test-in-android-studio#run-multiple-devices-parallel)
these devices.

| Device  | API | Services |
|---------|-----|----------|
| Nexus 5 | 19  | AOSP     |
| Nexus 5 | 21  | AOSP     |
| Nexus 5 | 22  | AOSP     |
| Pixel   | 25  | AOSP     |
| Pixel 2 | 27  | AOSP     |
| Pixel 2 | 34  | AOSP     |
| Pixel 2 | 35  | AOSP     |

## Device Configuration

### ```android.permission.ACCESS_MOCK_LOCATION```

When executing tests on devices with API less than `22`, mock location must be manually enabled
in [`Developer Options`](https://developer.android.com/studio/debug/dev-options).
