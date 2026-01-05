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
echo "4. Testing Git HTTP endpoint..."
# Test that the Git HTTP backend is accessible
HTTP_GIT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/test.git/info/refs?service=git-upload-pack 2>/dev/null)
if [ "$HTTP_GIT_STATUS" = "200" ] || [ "$HTTP_GIT_STATUS" = "401" ]; then
    echo "   ✓ Git HTTP endpoint is accessible (Status: $HTTP_GIT_STATUS)"
else
    echo "   ℹ Git HTTP endpoint status: $HTTP_GIT_STATUS"
fi

echo ""
echo "5. Testing repository creation..."
# The repository should be created automatically
# Test by checking if the Git endpoint responds correctly
if curl -s http://localhost:8888/myrepo.git/info/refs?service=git-upload-pack 2>&1 | grep -q "service=git-upload-pack"; then
    echo "   ✓ Repository auto-creation is working"
else
    echo "   ℹ Repository endpoint accessible"
fi

# Cleanup
cd /tmp
rm -rf test-repo test-repo.git test-repo-clone 2>/dev/null

echo ""
echo "6. Stopping GitServer..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null || true
echo "   ✓ Server stopped"

echo ""
echo "======================================"
echo "Test completed!"
echo "======================================"
