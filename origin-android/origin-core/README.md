# Core Origin Android SDK

This module implements the core Origin SDK functionality, used by other modules. This module will
usually not be used alone, instead it will be coupled with other modules, such as
`origin-antifraud` or `origin-ads`.

## Features

- Extensible module support.

- Event bus for propagating module generated events.

- Device connectivity descriptions.

- Device hardware and software descriptions.

- Descriptions of legal regulations applicable to the device.

- Lightweight, battery-efficient location descriptions.

- Origin remote procedure call (RPC) functionality.

## Using

### Dependency

Include `origin-core` as a dependency within `build.gradle`:

```groovy
// build.gradle

dependencies {
	implementation("org.poly-gamma.android.origin:origin-core:1.1.3")
}
```

### Initialization

Initialize `Origin` within application.

```java
import android.app.Application;

import org.polygamma.android.origin.core.Origin;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Origin.initialize(this);
    }
}
```

### Optional - Set Identifiers

The backend services used by all Origin services operate on application service requests in order
of:

1. Application identifier.
2. Publisher identifier *and* application bundle.
3. Application bundle.

The preferred identifier is the application identifier, since it is globally unique. In the case,
however, when the publisher distributes only unique application bundles, the publisher identifier
can be used instead. When the application bundle is globally unique, for example, because it may be
distributed only through a central app store, such as Google Play, the application bundle can be
used instead.

It should, however, be considered best practice to define either an application or publisher
identifier, to ensure application bundle spoofing is avoided. To define either an application or
publisher identifier, the `poly-gamma.origin.application-id` or `poly-gamma.origin.publisher-id`
meta tags can be defined within the application manifest.

```xml
<!-- AndroidManifest.xml -->
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="my.application"
>
    <application>
        <meta-data
            android:name="poly-gamma.origin.application-id"
			android:value="xxxxxxxx"
        />
		<!-- OR: -->
		<meta-data
			android:name="poly-gamma.origin.publisher-id"
			android:value="xxxxxxxx"
		/>
    </application>
</manifest>
```

## License

This project is licensed under [Apache-2.0][apache-2.0] **OR** [MIT][mit] license.

[apache-2.0]: ../LICENSES/Apache-2.0
[mit]: ../LICENSES/MIT
