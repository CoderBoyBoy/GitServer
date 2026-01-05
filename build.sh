#!/bin/bash
# Build script for GitServer

set -e

echo "Building GitServer..."
echo "===================="

# Compile the code
echo "Step 1: Compiling Java code..."
mvn clean compile

# Create target directory structure
echo "Step 2: Preparing JAR structure..."
mkdir -p target/jar-build
cd target/jar-build

# Extract all dependencies
# Note: We use a manual extraction approach instead of Maven's dependency:copy-dependencies
# because the Maven shade plugin has issues with certain dependency downloads in this environment.
# This ensures all required JARs (JGit, Jetty, SSHD, BouncyCastle, etc.) are included.
echo "Step 3: Extracting dependencies..."
for jar in $(find ~/.m2/repository -name "*.jar" | grep -E "(jgit|jetty|sshd|slf4j|jakarta.*servlet|bcprov|bcpg|bcpkix|bcutil|JavaEWAH|commons-codec)" | grep -v "sources\|javadoc" | grep -v "9.4.54" | grep -v "2.16.0"); do
    echo "  Extracting: $(basename $jar)"
    unzip -q -o "$jar" 2>/dev/null || true
done

# Copy compiled classes
echo "Step 4: Copying compiled classes..."
cp -r ../classes/* .

# Remove signature files
echo "Step 5: Cleaning signature files..."
find . -name "*.SF" -o -name "*.DSA" -o -name "*.RSA" | xargs rm -f 2>/dev/null || true

# Create manifest
echo "Step 6: Creating manifest..."
cat > manifest.txt << 'EOF'
Manifest-Version: 1.0
Main-Class: com.gitserver.GitServerApplication
EOF

# Build JAR
echo "Step 7: Building JAR..."
cd ..
jar cfm gitserver-1.0.0.jar jar-build/manifest.txt -C jar-build/ .

# Cleanup
rm -rf jar-build

echo ""
echo "===================="
echo "Build complete!"
echo "JAR file: target/gitserver-1.0.0.jar"
echo "Size: $(du -h target/gitserver-1.0.0.jar | cut -f1)"
echo ""
echo "To run:"
echo "  java -jar target/gitserver-1.0.0.jar"
echo ""

# Show actual size
if [ -f target/gitserver-1.0.0.jar ]; then
    echo "JAR size: $(ls -lh target/gitserver-1.0.0.jar | awk '{print $5}')"
fi
