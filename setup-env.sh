#!/bin/bash
# Environment setup script for NewPipe development
# Run this after an environment reset to configure Java, Android SDK, and Gradle proxy.
# Usage: bash setup-env.sh

set -euo pipefail

ANDROID_SDK_DIR="/opt/android-sdk"
GRADLE_PROPS="$HOME/.gradle/gradle.properties"

# ---------------------------------------------------------------------------
# 1. Install Java 17 (required by the project's toolchain config)
# ---------------------------------------------------------------------------
if ! /usr/lib/jvm/java-17-openjdk-amd64/bin/java -version &>/dev/null; then
    echo ">>> Installing OpenJDK 17..."
    apt-get update -qq && apt-get install -y -qq openjdk-17-jdk
else
    echo ">>> OpenJDK 17 already installed."
fi

# ---------------------------------------------------------------------------
# 2. Fix Gradle wrapper (download via curl if Java can't reach the internet)
# ---------------------------------------------------------------------------
GRADLE_WRAPPER_PROPS="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$GRADLE_WRAPPER_PROPS" ]; then
    GRADLE_VERSION=$(grep distributionUrl "$GRADLE_WRAPPER_PROPS" | sed 's/.*gradle-\(.*\)-bin.zip/\1/')
    GRADLE_DIST_DIR="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

    if [ -n "$GRADLE_VERSION" ] && ! find "$GRADLE_DIST_DIR" -maxdepth 2 -name "gradle-${GRADLE_VERSION}" -type d 2>/dev/null | grep -q .; then
        echo ">>> Downloading Gradle $GRADLE_VERSION via curl..."
        # Find or create the hash directory
        HASH_DIR=$(find "$GRADLE_DIST_DIR" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | head -1)
        if [ -z "$HASH_DIR" ]; then
            HASH_DIR="$GRADLE_DIST_DIR/manual"
            mkdir -p "$HASH_DIR"
        fi
        # Clean up partial downloads
        rm -f "$HASH_DIR"/*.lck "$HASH_DIR"/*.part
        if [ ! -f "$HASH_DIR/gradle-${GRADLE_VERSION}-bin.zip" ]; then
            curl -L -o "$HASH_DIR/gradle-${GRADLE_VERSION}-bin.zip" \
                "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
        fi
        if [ ! -d "$HASH_DIR/gradle-${GRADLE_VERSION}" ]; then
            unzip -q "$HASH_DIR/gradle-${GRADLE_VERSION}-bin.zip" -d "$HASH_DIR"
        fi
        echo ">>> Gradle $GRADLE_VERSION ready."
    else
        echo ">>> Gradle $GRADLE_VERSION already cached."
    fi
fi

# ---------------------------------------------------------------------------
# 3. Configure Gradle proxy from environment variables
# ---------------------------------------------------------------------------
configure_gradle_proxy() {
    local proxy_url="${https_proxy:-${HTTPS_PROXY:-${http_proxy:-${HTTP_PROXY:-}}}}"
    if [ -z "$proxy_url" ]; then
        echo ">>> No proxy detected in environment, skipping Gradle proxy config."
        return
    fi

    echo ">>> Configuring Gradle proxy settings..."
    mkdir -p "$(dirname "$GRADLE_PROPS")"

    # Parse proxy URL: http://user:pass@host:port
    local host port user pass
    host=$(python3 -c "from urllib.parse import urlparse; print(urlparse('$proxy_url').hostname)")
    port=$(python3 -c "from urllib.parse import urlparse; print(urlparse('$proxy_url').port)")
    user=$(python3 -c "from urllib.parse import urlparse; p=urlparse('$proxy_url'); print(p.username or '')")
    pass=$(python3 -c "from urllib.parse import urlparse; p=urlparse('$proxy_url'); print(p.password or '')")

    local no_proxy_pipes
    no_proxy_pipes=$(echo "${no_proxy:-localhost,127.0.0.1}" \
        | sed 's/,\*\.googleapis\.com//g; s/\*\.googleapis\.com,//g; s/\*\.googleapis\.com//g' \
        | sed 's/,\*\.google\.com//g; s/\*\.google\.com,//g; s/\*\.google\.com//g' \
        | tr ',' '|')

    cat > "$GRADLE_PROPS" <<PROPS
systemProp.http.proxyHost=$host
systemProp.http.proxyPort=$port
systemProp.http.proxyUser=$user
systemProp.http.proxyPassword=$pass
systemProp.http.nonProxyHosts=$no_proxy_pipes
systemProp.https.proxyHost=$host
systemProp.https.proxyPort=$port
systemProp.https.proxyUser=$user
systemProp.https.proxyPassword=$pass
systemProp.https.nonProxyHosts=$no_proxy_pipes
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
android.builder.sdkDownload=false
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8 -Djdk.http.auth.tunneling.disabledSchemes= -Djdk.http.auth.proxying.disabledSchemes=
PROPS
    echo ">>> Gradle proxy config written to $GRADLE_PROPS"
}
configure_gradle_proxy

# ---------------------------------------------------------------------------
# 4. Install Android SDK components
# ---------------------------------------------------------------------------
install_sdk_component() {
    local label="$1" dest="$2" url="$3"
    if [ -d "$dest" ]; then
        echo ">>> $label already installed."
        return
    fi
    echo ">>> Installing $label..."
    local tmpzip
    tmpzip=$(mktemp /tmp/sdk-XXXXXX.zip)
    curl -L -o "$tmpzip" "$url"
    mkdir -p "$(dirname "$dest")"
    unzip -q "$tmpzip" -d "$(dirname "$dest")"
    # The zip may extract to a different directory name; rename if needed
    if [ ! -d "$dest" ]; then
        local extracted
        extracted=$(find "$(dirname "$dest")" -maxdepth 1 -mindepth 1 -type d -newer "$(dirname "$dest")" | head -1)
        if [ -n "$extracted" ] && [ "$extracted" != "$dest" ]; then
            mv "$extracted" "$dest"
        fi
    fi
    rm -f "$tmpzip"
    echo ">>> $label installed."
}

# Command-line tools
if [ ! -d "$ANDROID_SDK_DIR/cmdline-tools/latest/bin" ]; then
    echo ">>> Installing Android SDK command-line tools..."
    mkdir -p "$ANDROID_SDK_DIR"
    tmpzip=$(mktemp /tmp/sdk-cmdline-XXXXXX.zip)
    curl -L -o "$tmpzip" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q "$tmpzip" -d "$ANDROID_SDK_DIR"
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools/latest"
    mv "$ANDROID_SDK_DIR/cmdline-tools/bin" \
       "$ANDROID_SDK_DIR/cmdline-tools/lib" \
       "$ANDROID_SDK_DIR/cmdline-tools/NOTICE.txt" \
       "$ANDROID_SDK_DIR/cmdline-tools/source.properties" \
       "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || true
    rm -f "$tmpzip"
    echo ">>> Command-line tools installed."
else
    echo ">>> Android SDK command-line tools already installed."
fi

# Accept licenses
mkdir -p "$ANDROID_SDK_DIR/licenses"
echo -e "\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_SDK_DIR/licenses/android-sdk-license"
echo -e "\n84831b9409646a918e30573bab4c9c91346d8abd" > "$ANDROID_SDK_DIR/licenses/android-sdk-preview-license"

# Read required SDK versions from build.gradle.kts
COMPILE_SDK=$(grep 'compileSdk' app/build.gradle.kts 2>/dev/null | head -1 | grep -oP '\d+' || echo "36")
# Detect required build-tools from AGP or use sensible defaults
BUILD_TOOLS_VERSIONS=("35.0.0" "36.0.0")

# Platform
install_sdk_component \
    "Android platform $COMPILE_SDK" \
    "$ANDROID_SDK_DIR/platforms/android-$COMPILE_SDK" \
    "https://dl.google.com/android/repository/platform-${COMPILE_SDK}_r02.zip"

# Build tools
for BT_VER in "${BUILD_TOOLS_VERSIONS[@]}"; do
    BT_MAJOR="${BT_VER%%.*}"
    install_sdk_component \
        "Build tools $BT_VER" \
        "$ANDROID_SDK_DIR/build-tools/$BT_VER" \
        "https://dl.google.com/android/repository/build-tools_r${BT_MAJOR}_linux.zip"
done

# ---------------------------------------------------------------------------
# 5. Create local.properties
# ---------------------------------------------------------------------------
echo "sdk.dir=$ANDROID_SDK_DIR" > local.properties
echo ">>> local.properties written."

# ---------------------------------------------------------------------------
# 6. Export environment variables (print for user to source)
# ---------------------------------------------------------------------------
export ANDROID_HOME="$ANDROID_SDK_DIR"
echo ""
echo "========================================="
echo "  Environment setup complete!"
echo "========================================="
echo ""
echo "To export ANDROID_HOME in your current shell, run:"
echo "  export ANDROID_HOME=$ANDROID_SDK_DIR"
echo ""
echo "You can now run:  ./gradlew test"
