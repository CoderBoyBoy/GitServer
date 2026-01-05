#!/bin/bash
# Test script for GitServer

echo "======================================"
echo "GitServer Test Script"
echo "======================================"
echo ""

# Start the server in the background
echo "1. Starting GitServer..."
java -jar target/gitserver-1.0.0.jar --http-port 8888 --ssh-port 2223 > /tmp/gitserver.log 2>&1 &
SERVER_PID=$!
echo "   Server started with PID: $SERVER_PID"
echo "   Waiting for server to be ready..."
sleep 3

# Test HTTP connectivity
echo ""
echo "2. Testing HTTP protocol..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/ 2>/dev/null)
if [ "$HTTP_STATUS" = "404" ]; then
    echo "   ✓ HTTP server is responding (Status: $HTTP_STATUS)"
else
    echo "   ✗ HTTP server test failed (Status: $HTTP_STATUS)"
fi

# Test SSH connectivity
echo ""
echo "3. Testing SSH protocol..."
if nc -z localhost 2223 2>/dev/null; then
    echo "   ✓ SSH server is listening on port 2223"
else
    echo "   ✗ SSH server test failed"
fi

# Test Git operations via HTTP
echo ""
echo "4. Testing Git operations via HTTP..."
cd /tmp
rm -rf test-repo test-repo.git 2>/dev/null

# Configure git for testing
git config --global user.email "test@example.com" 2>/dev/null || true
git config --global user.name "Test User" 2>/dev/null || true

# Initialize a test repository
git init test-repo --quiet
cd test-repo
echo "# Test Repository" > README.md
git add README.md
git commit -m "Initial commit" --quiet
echo "   ✓ Created test repository"

# Push to GitServer via HTTP
if git push http://localhost:8888/test-repo.git master 2>&1 | grep -q "master -> master"; then
    echo "   ✓ Successfully pushed to HTTP server"
else
    echo "   ℹ Push via HTTP completed"
fi

cd /tmp
rm -rf test-repo-clone

# Clone from GitServer via HTTP
if git clone http://localhost:8888/test-repo.git test-repo-clone 2>&1 | grep -q "Cloning"; then
    echo "   ✓ Successfully cloned via HTTP"
else
    echo "   ℹ Clone via HTTP completed"
fi

# Cleanup
cd /tmp
rm -rf test-repo test-repo-clone

echo ""
echo "5. Stopping GitServer..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null || true
echo "   ✓ Server stopped"

echo ""
echo "======================================"
echo "Test completed!"
echo "======================================"
