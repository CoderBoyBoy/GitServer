#!/bin/bash
# Quick demo of GitServer functionality

echo "========================================="
echo "GitServer Quick Demo"
echo "========================================="
echo ""

# Check if JAR exists
if [ ! -f "target/gitserver-1.0.0.jar" ]; then
    echo "Building GitServer..."
    ./build.sh > /dev/null 2>&1
fi

echo "Starting GitServer (HTTP: 8080, SSH: 2222)..."
java -jar target/gitserver-1.0.0.jar > /tmp/demo-server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

if ! ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "❌ Server failed to start. Check logs:"
    cat /tmp/demo-server.log
    exit 1
fi

echo "✅ Server running (PID: $SERVER_PID)"
echo ""

# Show server info
echo "📡 Server endpoints:"
echo "   HTTP: http://localhost:8080/"
echo "   SSH:  ssh://git@localhost:2222/"
echo ""

echo "💡 Try these commands in another terminal:"
echo ""
echo "   # Clone a repository via HTTP:"
echo "   git clone http://localhost:8080/my-project.git"
echo ""
echo "   # Clone a repository via SSH:"
echo "   git clone ssh://git@localhost:2222/my-project.git"
echo ""
echo "   # Repositories are created automatically on first access"
echo "   # and stored in ./repositories/ directory"
echo ""

echo "Press Ctrl+C to stop the server..."
echo ""

# Keep script running and forward signals
trap "echo ''; echo 'Stopping server...'; kill $SERVER_PID 2>/dev/null; exit 0" INT TERM

# Wait for server process
wait $SERVER_PID 2>/dev/null
